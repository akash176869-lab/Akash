package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiLiveSession
import com.example.audio.AudioLiveClient
import com.example.bridge.AndroidActionManager
import com.example.bridge.AndroidBridge
import com.example.model.ActionResult
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.ConnectionStatus
import com.example.model.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ArushiViewModel"
    }

    val actionManager = AndroidActionManager(application)

    val androidBridge = AndroidBridge(actionManager) { result ->
        recordActionResult(result)
    }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _inputVolume = MutableStateFlow(0f)
    val inputVolume: StateFlow<Float> = _inputVolume.asStateFlow()

    private val _outputVolume = MutableStateFlow(0f)
    val outputVolume: StateFlow<Float> = _outputVolume.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _actionHistory = MutableStateFlow<List<ActionResult>>(emptyList())
    val actionHistory: StateFlow<List<ActionResult>> = _actionHistory.asStateFlow()

    private val _detectedLanguage = MutableStateFlow("Auto-Detect")
    val detectedLanguage: StateFlow<String> = _detectedLanguage.asStateFlow()

    private val _selectedVoice = MutableStateFlow("Aoede")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private var audioClient: AudioLiveClient? = null
    private var liveSession: GeminiLiveSession? = null

    init {
        initAudioAndSession()
    }

    private fun initAudioAndSession() {
        val apiKey = BuildConfig.GEMINI_API_KEY

        audioClient = AudioLiveClient(
            scope = viewModelScope,
            onAudioChunkRecorded = { b64 ->
                liveSession?.sendAudioChunk(b64)
            },
            onInputVolumeChanged = { vol ->
                _inputVolume.value = vol
                if (vol > 0.05f && _assistantState.value != AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.LISTENING
                }
            },
            onOutputVolumeChanged = { vol ->
                _outputVolume.value = vol
            },
            onPlaybackStateChanged = { isPlaying ->
                if (isPlaying) {
                    _assistantState.value = AssistantState.SPEAKING
                } else if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        )

        liveSession = GeminiLiveSession(
            scope = viewModelScope,
            apiKey = apiKey,
            onConnectionStatusChanged = { status ->
                _connectionStatus.value = status
                if (status == ConnectionStatus.CONNECTED) {
                    audioClient?.initAudioTrack()
                } else if (status == ConnectionStatus.ERROR) {
                    _assistantState.value = AssistantState.ERROR
                }
            },
            onAudioChunkReceived = { pcm ->
                audioClient?.enqueueAudioData(pcm)
            },
            onTextReceived = { text, isTurnComplete ->
                if (text.isNotBlank()) {
                    _currentStreamingText.value += text
                    val detected = detectLanguage(_currentStreamingText.value)
                    _detectedLanguage.value = detected
                }
                if (isTurnComplete) {
                    val fullText = _currentStreamingText.value.trim()
                    if (fullText.isNotBlank()) {
                        val lang = detectLanguage(fullText)
                        addMessage(ChatMessage(sender = MessageSender.ARUSHI, text = fullText, language = lang))
                    }
                    _currentStreamingText.value = ""
                }
            },
            onInterrupted = {
                audioClient?.stopAndFlushAudio()
                _currentStreamingText.value = ""
                _assistantState.value = AssistantState.LISTENING
            },
            onToolCallReceived = { id, name, args, sendResponse ->
                handleToolExecution(id, name, args, sendResponse)
            }
        )
    }

    fun connect() {
        liveSession?.connect(_selectedVoice.value)
    }

    fun disconnect() {
        audioClient?.stopRecording()
        liveSession?.disconnect()
        _assistantState.value = AssistantState.IDLE
    }

    fun startListening(): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
            connect()
        }
        val started = audioClient?.startRecording() == true
        if (started) {
            _assistantState.value = AssistantState.LISTENING
        }
        return started
    }

    fun stopListening() {
        audioClient?.stopRecording()
        _assistantState.value = AssistantState.IDLE
    }

    fun toggleListening(): Boolean {
        return if (_assistantState.value == AssistantState.LISTENING) {
            stopListening()
            false
        } else {
            startListening()
        }
    }

    fun sendTextCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return

        val lang = detectLanguage(trimmed)
        _detectedLanguage.value = lang
        addMessage(ChatMessage(sender = MessageSender.USER, text = trimmed, language = lang))

        _assistantState.value = AssistantState.THINKING

        if (_connectionStatus.value != ConnectionStatus.CONNECTED) {
            connect()
        }

        liveSession?.sendTextTurn(trimmed)
    }

    private fun handleToolExecution(
        callId: String,
        name: String,
        args: JSONObject,
        sendResponse: (JSONObject) -> Unit
    ) {
        _assistantState.value = AssistantState.EXECUTING_ACTION
        Log.d(TAG, "Executing tool: $name with args: $args")

        val result: ActionResult = when (name) {
            "openWhatsApp" -> actionManager.openWhatsApp()
            "openApp" -> {
                val appName = args.optString("appName", "")
                actionManager.openApp(appName)
            }
            "makeCall" -> {
                val phoneNumber = args.optString("phoneNumber", "")
                actionManager.makeCall(phoneNumber)
            }
            "callContact" -> {
                val contactName = args.optString("contactName", "")
                actionManager.callContact(contactName)
            }
            "openUrl" -> {
                val url = args.optString("url", "")
                actionManager.openUrl(url)
            }
            else -> {
                ActionResult(
                    actionType = name,
                    isSuccess = false,
                    message = "Unknown tool: $name"
                )
            }
        }

        recordActionResult(result)

        // Return tool response to Gemini
        val output = JSONObject().apply {
            put("status", if (result.isSuccess) "success" else "error")
            put("message", result.message)
            if (result.details != null) {
                put("details", result.details)
            }
            if (result.matchedContacts.isNotEmpty()) {
                val names = result.matchedContacts.joinToString { "${it.name} (${it.phoneNumber})" }
                put("matchedContacts", names)
            }
        }

        sendResponse(output)
    }

    private fun recordActionResult(result: ActionResult) {
        val updatedHistory = listOf(result) + _actionHistory.value
        _actionHistory.value = updatedHistory

        addMessage(
            ChatMessage(
                sender = MessageSender.SYSTEM,
                text = result.message,
                actionResult = result
            )
        )
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun clearHistory() {
        _messages.value = emptyList()
        _actionHistory.value = emptyList()
    }

    fun setVoice(voice: String) {
        _selectedVoice.value = voice
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            disconnect()
            connect()
        }
    }

    private fun detectLanguage(text: String): String {
        val devanagariCount = text.count { it in '\u0900'..'\u097F' }
        val bengaliCount = text.count { it in '\u0980'..'\u09FF' }
        val gurmukhiCount = text.count { it in '\u0A00'..'\u0A7F' }
        val gujaratiCount = text.count { it in '\u0A80'..'\u0AFF' }
        val tamilCount = text.count { it in '\u0B80'..'\u0BFF' }
        val teluguCount = text.count { it in '\u0C00'..'\u0C7F' }
        val kannadaCount = text.count { it in '\u0C80'..'\u0CFF' }
        val malayalamCount = text.count { it in '\u0D00'..'\u0D7F' }
        val urduCount = text.count { it in '\u0600'..'\u06FF' }

        if (devanagariCount > 2) return "Hindi"
        if (bengaliCount > 2) return "Bengali"
        if (gujaratiCount > 2) return "Gujarati"
        if (tamilCount > 2) return "Tamil"
        if (teluguCount > 2) return "Telugu"
        if (kannadaCount > 2) return "Kannada"
        if (malayalamCount > 2) return "Malayalam"
        if (gurmukhiCount > 2) return "Punjabi"
        if (urduCount > 2) return "Urdu"

        val lower = text.lowercase()
        val hinglishWords = listOf(
            "kholo", "karo", "kaise", "hai", "mein", "baat", "bolo", "lagao", "chalao", "sunao",
            "mera", "meri", "hum", "aap", "tum", "kya", "kyun", "kab", "kahan", "namaste",
            "shukriya", "bhai", "yaar", "theek", "mummy", "papa", "didi", "achha", "batao"
        )
        if (hinglishWords.any { lower.contains(it) }) {
            return "Hinglish"
        }

        return "English"
    }

    override fun onCleared() {
        super.onCleared()
        audioClient?.release()
        liveSession?.disconnect()
    }
}
