package com.example.model

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING_ACTION,
    ERROR
}

data class ContactMatch(
    val name: String,
    val phoneNumber: String,
    val contactId: String = ""
)

data class ActionResult(
    val actionType: String,
    val isSuccess: Boolean,
    val message: String,
    val details: String? = null,
    val matchedContacts: List<ContactMatch> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val language: String? = null,
    val actionResult: ActionResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ARUSHI,
    SYSTEM
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
