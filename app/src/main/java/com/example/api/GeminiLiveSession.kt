package com.example.api

import android.util.Base64
import android.util.Log
import com.example.model.ActionResult
import com.example.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveSession(
    private val scope: CoroutineScope,
    private val apiKey: String,
    private val onConnectionStatusChanged: (ConnectionStatus) -> Unit,
    private val onAudioChunkReceived: (ByteArray) -> Unit,
    private val onTextReceived: (String, Boolean) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onToolCallReceived: (id: String, name: String, args: JSONObject, sendResponse: (JSONObject) -> Unit) -> Unit
) {
    companion object {
        private const val TAG = "GeminiLiveSession"
        private const val LIVE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    fun connect(voiceName: String = "Aoede") {
        if (apiKey.isBlank()) {
            onConnectionStatusChanged(ConnectionStatus.ERROR)
            return
        }

        onConnectionStatusChanged(ConnectionStatus.CONNECTING)
        val url = "$LIVE_WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened.")
                scope.launch(Dispatchers.Main) {
                    onConnectionStatusChanged(ConnectionStatus.CONNECTED)
                }
                sendSetupMessage(ws, voiceName)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Gemini Live closing: $code $reason")
                scope.launch(Dispatchers.Main) {
                    onConnectionStatusChanged(ConnectionStatus.DISCONNECTED)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Gemini Live failure: ${t.localizedMessage}", t)
                scope.launch(Dispatchers.Main) {
                    onConnectionStatusChanged(ConnectionStatus.ERROR)
                }
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket, voiceName: String) {
        val setupJson = JSONObject()
        val setupObj = JSONObject()

        // Model
        setupObj.put("model", "models/gemini-2.0-flash-exp")

        // Generation Config
        val genConfig = JSONObject()
        val modalities = JSONArray().apply { put("AUDIO") }
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuilt = JSONObject().apply { put("voiceName", voiceName) }
        voiceConfig.put("prebuiltVoiceConfig", prebuilt)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)
        setupObj.put("generationConfig", genConfig)

        // System Instruction
        val systemInstruction = JSONObject()
        val partsArray = JSONArray()
        val instructionPart = JSONObject().apply {
            put(
                "text",
                """
                You are Arushi, an exceptionally smart, warm, and natural AI voice assistant on Android.
                
                LANGUAGE CAPABILITIES (CRITICAL):
                - You are fully multilingual and naturally understand and speak multiple Indian languages and English:
                  Hindi, English, Hinglish, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, and others.
                - Automatically detect the language the user is speaking in without asking them to select one.
                - If the user speaks Hindi, answer naturally in Hindi.
                - If the user speaks English, answer fluently in English.
                - If the user speaks Hinglish (e.g., 'kaise ho', 'kya haal hai', 'WhatsApp open karo'), reply naturally in Hinglish.
                - Seamlessly switch languages mid-conversation if the user switches languages.
                - Never use robotic speech. Keep responses conversational, concise, friendly, and appropriate for audio listening.
                
                DEVICE CONTROL & FUNCTION CALLING (CRITICAL):
                - You have direct tools to control the phone:
                  1. openWhatsApp(): Call when user asks to open WhatsApp ('WhatsApp kholo', 'open WhatsApp', 'WhatsApp open karo', 'WhatsApp chalao', 'open my WhatsApp').
                  2. openApp(appName): Call when user asks to open apps like YouTube, Instagram, Chrome, Settings, Camera, Maps, Spotify, etc.
                  3. makeCall(phoneNumber): Call when user asks to call a specific phone number (e.g. 'Call 9876543210').
                  4. callContact(contactName): Call when user asks to call someone by name (e.g. 'Call Mom', 'Call Mummy', 'Call Rahul', 'Mummy ko phone lagao', 'Rahul ko call karo', 'Call Dad').
                  5. openUrl(url): Call when user asks to open a website or URL.
                - ALWAYS execute the tool when asked. Do NOT merely say you did it without calling the tool!
                - After the tool returns its result, acknowledge it briefly and naturally in the user's spoken language.
                - If a contact is not found or multiple contacts are found, explain clearly and ask for clarification.
                """.trimIndent()
            )
        }
        partsArray.put(instructionPart)
        systemInstruction.put("parts", partsArray)
        setupObj.put("systemInstruction", systemInstruction)

        // Tools Declaration
        val toolsArray = JSONArray()
        val toolObj = JSONObject()
        val functionDeclarations = JSONArray()

        // 1. openWhatsApp
        val openWhatsAppFunc = JSONObject().apply {
            put("name", "openWhatsApp")
            put("description", "Opens WhatsApp messenger on the user's Android phone.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        }
        functionDeclarations.put(openWhatsAppFunc)

        // 2. openApp
        val openAppFunc = JSONObject().apply {
            put("name", "openApp")
            put("description", "Opens an installed Android app by name, such as YouTube, Instagram, Chrome, Settings, Camera, Maps, Spotify, etc.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("appName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the app to launch (e.g. 'YouTube', 'Instagram', 'Chrome', 'Settings', 'Camera', 'Spotify', 'Maps').")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("appName") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(openAppFunc)

        // 3. makeCall
        val makeCallFunc = JSONObject().apply {
            put("name", "makeCall")
            put("description", "Initiates or dials a phone call to a given phone number.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("phoneNumber", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The telephone number to dial or call.")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("phoneNumber") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(makeCallFunc)

        // 4. callContact
        val callContactFunc = JSONObject().apply {
            put("name", "callContact")
            put("description", "Looks up a person's contact name or relationship in the user's phonebook and initiates a phone call (e.g. 'Mom', 'Mummy', 'Rahul', 'Dad').")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("contactName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The contact name or relation (e.g. 'Mom', 'Mummy', 'Rahul', 'Dad').")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("contactName") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(callContactFunc)

        // 5. openUrl
        val openUrlFunc = JSONObject().apply {
            put("name", "openUrl")
            put("description", "Opens a website URL in the device browser.")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("url", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The destination web URL.")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("url") })
            }
            put("parameters", params)
        }
        functionDeclarations.put(openUrlFunc)

        toolObj.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolObj)
        setupObj.put("tools", toolsArray)

        setupJson.put("setup", setupObj)

        Log.d(TAG, "Sending setup message...")
        ws.send(setupJson.toString())
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val root = JSONObject(text)

            // 1. Server Content (Audio / Text / Interruption)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Arushi was interrupted by user speech.")
                    scope.launch(Dispatchers.Main) {
                        onInterrupted()
                    }
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Check audio data
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val mimeType = inlineData.optString("mimeType", "")
                                val b64Data = inlineData.optString("data", "")
                                if (mimeType.startsWith("audio/") && b64Data.isNotBlank()) {
                                    val audioBytes = Base64.decode(b64Data, Base64.NO_WRAP)
                                    onAudioChunkReceived(audioBytes)
                                }
                            }

                            // Check text transcript
                            if (part.has("text")) {
                                val transcriptText = part.getString("text")
                                scope.launch(Dispatchers.Main) {
                                    onTextReceived(transcriptText, false)
                                }
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    scope.launch(Dispatchers.Main) {
                        onTextReceived("", true)
                    }
                }
            }

            // 2. Tool Calls
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val callObj = functionCalls.getJSONObject(i)
                        val id = callObj.getString("id")
                        val name = callObj.getString("name")
                        val args = callObj.optJSONObject("args") ?: JSONObject()

                        Log.d(TAG, "Received Tool Call: $name (id=$id)")
                        scope.launch(Dispatchers.Main) {
                            onToolCallReceived(id, name, args) { outputObj ->
                                sendToolResponse(id, outputObj)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.localizedMessage}", e)
        }
    }

    fun sendAudioChunk(base64Pcm16k: String) {
        val ws = webSocket ?: return
        try {
            val root = JSONObject()
            val realtimeInput = JSONObject()
            val mediaChunks = JSONArray()
            val chunk = JSONObject().apply {
                put("mimeType", "audio/pcm;rate=16000")
                put("data", base64Pcm16k)
            }
            mediaChunks.put(chunk)
            realtimeInput.put("mediaChunks", mediaChunks)
            root.put("realtimeInput", realtimeInput)

            ws.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk: ${e.localizedMessage}")
        }
    }

    fun sendTextTurn(prompt: String) {
        val ws = webSocket ?: return
        try {
            val root = JSONObject()
            val clientContent = JSONObject()
            val turns = JSONArray()
            val turn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                }
                put("parts", parts)
            }
            turns.put(turn)
            clientContent.put("turns", turns)
            clientContent.put("turnComplete", true)
            root.put("clientContent", clientContent)

            ws.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text turn: ${e.localizedMessage}")
        }
    }

    fun sendToolResponse(callId: String, output: JSONObject) {
        val ws = webSocket ?: return
        try {
            val root = JSONObject()
            val toolResponse = JSONObject()
            val functionResponses = JSONArray()
            val fnResponse = JSONObject().apply {
                put("id", callId)
                val respObj = JSONObject().apply {
                    put("output", output)
                }
                put("response", respObj)
            }
            functionResponses.put(fnResponse)
            toolResponse.put("functionResponses", functionResponses)
            root.put("toolResponse", toolResponse)

            Log.d(TAG, "Sending tool response for callId=$callId: $output")
            ws.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response: ${e.localizedMessage}")
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTED)
    }
}
