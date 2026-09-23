package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class AndroidSpeechRecognizerManager(
    private val context: Context,
    private val onTextRecognized: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "SpeechRecognizerMgr"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow(SpeechRecognizerState())
    val state: StateFlow<SpeechRecognizerState> = _state.asStateFlow()

    private var targetLanguage: String = "auto"
    private var isContinuous: Boolean = false
    private var accumulatedText: StringBuilder = StringBuilder()

    init {
        mainHandler.post {
            checkAvailability()
        }
    }

    private fun checkAvailability(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        _state.update { it.copy(isAvailable = available) }
        return available
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null) {
            if (!checkAvailability()) {
                Log.w(TAG, "SpeechRecognizer is not available on this device")
                return null
            }
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
                _state.update { it.copy(error = "Failed to initialize SpeechRecognizer: ${e.localizedMessage}") }
                return null
            }
        }
        return speechRecognizer
    }

    fun setLanguage(languageCode: String) {
        targetLanguage = languageCode
        _state.update { it.copy(selectedLanguage = languageCode) }
    }

    fun setContinuousMode(enabled: Boolean) {
        isContinuous = enabled
        _state.update { it.copy(continuousMode = enabled) }
    }

    fun startListening(
        languageCode: String? = null,
        continuous: Boolean? = null
    ) {
        if (languageCode != null) {
            setLanguage(languageCode)
        }
        if (continuous != null) {
            setContinuousMode(continuous)
        }

        mainHandler.post {
            val recognizer = getOrCreateRecognizer()
            if (recognizer == null) {
                _state.update { it.copy(error = "Speech recognition is not available or disabled.") }
                return@post
            }

            try {
                recognizer.cancel()
            } catch (_: Exception) {}

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                // Request real-time partial results as speech occurs
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                val selected = targetLanguage
                if (selected != "auto" && selected.isNotBlank()) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, selected)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selected)
                } else {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                }
            }

            _state.update {
                it.copy(
                    isListening = true,
                    isReady = false,
                    partialText = "",
                    error = null
                )
            }

            try {
                recognizer.startListening(intent)
                Log.d(TAG, "Started SpeechRecognizer listening (lang: $targetLanguage)")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer", e)
                _state.update {
                    it.copy(
                        isListening = false,
                        error = "Error starting recognition: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
            _state.update { it.copy(isListening = false, isReady = false, rmsLevel = 0f) }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling SpeechRecognizer", e)
            }
            _state.update {
                it.copy(
                    isListening = false,
                    isReady = false,
                    partialText = "",
                    rmsLevel = 0f
                )
            }
        }
    }

    fun clear() {
        accumulatedText.clear()
        _state.update {
            it.copy(
                partialText = "",
                finalText = "",
                allResults = emptyList(),
                error = null
            )
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                _state.update { it.copy(isReady = true, error = null) }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                _state.update { it.copy(isReady = true) }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB typically ranges from -2 dB up to 10+ dB
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _state.update { it.copy(rmsLevel = normalized) }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                _state.update { it.copy(isReady = false, rmsLevel = 0f) }
            }

            override fun onError(errorCode: Int) {
                val errorMessage = resolveErrorMessage(errorCode)
                Log.w(TAG, "SpeechRecognizer error: $errorCode ($errorMessage)")

                val isIgnorable = errorCode == SpeechRecognizer.ERROR_NO_MATCH ||
                        errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                _state.update {
                    it.copy(
                        isListening = if (isContinuous) it.isListening else false,
                        isReady = false,
                        rmsLevel = 0f,
                        error = if (isIgnorable) null else errorMessage
                    )
                }

                if (isContinuous && _state.value.isListening) {
                    mainHandler.postDelayed({
                        if (_state.value.isListening) {
                            startListening()
                        }
                    }, 400)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topMatch = matches?.firstOrNull()?.trim().orEmpty()
                Log.d(TAG, "onResults: topMatch='$topMatch', allMatches=$matches")

                if (topMatch.isNotBlank()) {
                    if (accumulatedText.isNotEmpty()) {
                        accumulatedText.append(" ")
                    }
                    accumulatedText.append(topMatch)
                    val full = accumulatedText.toString()

                    _state.update {
                        it.copy(
                            partialText = "",
                            finalText = full,
                            allResults = matches ?: emptyList(),
                            rmsLevel = 0f,
                            isListening = if (isContinuous) true else false,
                            isReady = false
                        )
                    }

                    onTextRecognized?.invoke(full)
                } else {
                    _state.update {
                        it.copy(
                            partialText = "",
                            rmsLevel = 0f,
                            isListening = if (isContinuous) true else false,
                            isReady = false
                        )
                    }
                }

                if (isContinuous && _state.value.isListening) {
                    mainHandler.postDelayed({
                        if (_state.value.isListening) {
                            startListening()
                        }
                    }, 300)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    Log.v(TAG, "onPartialResults: $partial")
                    _state.update { it.copy(partialText = partial) }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun resolveErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Client error. Please try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout while processing speech."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak closer to the mic."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please wait a moment."
            SpeechRecognizer.ERROR_SERVER -> "Speech server encountered an error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input heard. Listening timed out."
            else -> "Speech recognition error code: $errorCode"
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
            speechRecognizer = null
        }
    }
}
