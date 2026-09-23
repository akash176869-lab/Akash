package com.example.speech

data class SpeechLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
)

val SUPPORTED_SPEECH_LANGUAGES = listOf(
    SpeechLanguage("auto", "Auto Detect", "Device Default", "🌐"),
    SpeechLanguage("hi-IN", "Hindi", "हिन्दी (भारत)", "🇮🇳"),
    SpeechLanguage("en-IN", "English", "English (India)", "🇮🇳"),
    SpeechLanguage("en-US", "English", "English (US)", "🇺🇸"),
    SpeechLanguage("mr-IN", "Marathi", "मराठी (महाराष्ट्र)", "🇮🇳"),
    SpeechLanguage("bn-IN", "Bengali", "বাংলা", "🇮🇳"),
    SpeechLanguage("gu-IN", "Gujarati", "ગુજરાતી", "🇮🇳"),
    SpeechLanguage("ta-IN", "Tamil", "தமிழ்", "🇮🇳"),
    SpeechLanguage("te-IN", "Telugu", "తెలుగు", "🇮🇳"),
    SpeechLanguage("kn-IN", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
    SpeechLanguage("ml-IN", "Malayalam", "മലയാളം", "🇮🇳"),
    SpeechLanguage("pa-IN", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
    SpeechLanguage("ur-IN", "Urdu", "اردو", "🇮🇳")
)

data class SpeechRecognizerState(
    val isListening: Boolean = false,
    val isAvailable: Boolean = true,
    val partialText: String = "",
    val finalText: String = "",
    val allResults: List<String> = emptyList(),
    val rmsLevel: Float = 0f,
    val error: String? = null,
    val selectedLanguage: String = "auto",
    val continuousMode: Boolean = false,
    val isReady: Boolean = false
) {
    val displayLiveText: String
        get() = if (partialText.isNotBlank()) {
            if (finalText.isNotBlank()) "$finalText $partialText" else partialText
        } else {
            finalText
        }
}
