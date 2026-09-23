package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speech.SUPPORTED_SPEECH_LANGUAGES
import com.example.speech.SpeechRecognizerState

@Composable
fun VoiceToTextScreen(
    speechState: SpeechRecognizerState,
    onStartListening: (String, Boolean) -> Unit,
    onStopListening: () -> Unit,
    onClear: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onSendToAssistant: (String) -> Unit,
    onExecuteAction: (String) -> Unit,
    onRequestPermission: () -> Unit,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val pulseTransition = rememberInfiniteTransition(label = "sttPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeMicColor by animateColorAsState(
        targetValue = if (speechState.isListening) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
        label = "activeMicColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Speech Recognizer API",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SpeechRecognizer API",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Real-time on-device voice-to-text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            !speechState.isAvailable -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            speechState.isListening -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                            else -> Color(0xFF10B981).copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        when {
                                            !speechState.isAvailable -> Color(0xFFEF4444)
                                            speechState.isListening -> Color(0xFF00E5FF)
                                            else -> Color(0xFF10B981)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !speechState.isAvailable -> "Unavailable"
                                    speechState.isListening -> "Listening..."
                                    else -> "Ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (!speechState.isAvailable) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Android SpeechRecognizer service is not available on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        // Live Real-Time Transcription Output Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("speech_results_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = borderFromState(speechState.isListening)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Audio Level",
                            tint = if (speechState.isListening) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (speechState.isListening) "Real-time Live Audio Stream" else "Transcription Output",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (speechState.finalText.isNotBlank() || speechState.partialText.isNotBlank()) {
                        Row {
                            IconButton(
                                onClick = {
                                    val full = speechState.displayLiveText
                                    clipboardManager.setText(AnnotatedString(full))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Audio level indicator when listening
                AnimatedVisibility(visible = speechState.isListening) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (speechState.isReady) "Listening for speech..." else "Starting audio capture...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = "Mic Level: ${(speechState.rmsLevel * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { speechState.rmsLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF00E5FF),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Text Box
                val hasAnyText = speechState.finalText.isNotBlank() || speechState.partialText.isNotBlank()
                if (hasAnyText) {
                    val annotatedText = buildAnnotatedString {
                        if (speechState.finalText.isNotBlank()) {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp
                                )
                            ) {
                                append(speechState.finalText)
                            }
                        }
                        if (speechState.partialText.isNotBlank()) {
                            if (speechState.finalText.isNotBlank()) {
                                append(" ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF00B0FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            ) {
                                append(speechState.partialText)
                            }
                        }
                    }

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("transcribed_live_text")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (speechState.isListening)
                                "Speak now... Hindi, English, Hinglish, Marathi, Tamil, etc."
                            else
                                "Tap the microphone below to start speaking in real time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Error notice if any
                if (speechState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = speechState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Alternate hypotheses chips
                if (speechState.allResults.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Alternative hypotheses:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        speechState.allResults.drop(1).forEach { hypothesis ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(hypothesis))
                                    Toast.makeText(context, "Copied candidate", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = hypothesis,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Quick Action Bar for Transcribed Text
                if (hasAnyText) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSendToAssistant(speechState.displayLiveText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_to_arushi_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send to Arushi", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onExecuteAction(speechState.displayLiveText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("execute_action_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Action", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Language Selection Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recognition Language",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Selected: ${SUPPORTED_SPEECH_LANGUAGES.find { it.code == speechState.selectedLanguage }?.displayName ?: speechState.selectedLanguage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val langScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(langScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SUPPORTED_SPEECH_LANGUAGES.forEach { lang ->
                    FilterChip(
                        selected = speechState.selectedLanguage == lang.code,
                        onClick = { onSelectLanguage(lang.code) },
                        label = {
                            Text("${lang.flag} ${lang.displayName}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Settings & Options Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Dictation Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keeps listening continuously across multiple sentences and pauses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = speechState.continuousMode,
                        onCheckedChange = { onToggleContinuous(it) },
                        modifier = Modifier.testTag("continuous_mode_switch")
                    )
                }
            }
        }

        // Main SpeechRecognizer Mic Control
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (speechState.isListening) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                )
            }

            FloatingActionButton(
                onClick = {
                    if (!hasMicPermission) {
                        onRequestPermission()
                    } else if (speechState.isListening) {
                        onStopListening()
                    } else {
                        onStartListening(speechState.selectedLanguage, speechState.continuousMode)
                    }
                },
                shape = CircleShape,
                containerColor = activeMicColor,
                contentColor = if (speechState.isListening) Color(0xFF00363A) else Color.White,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("speech_recognizer_main_mic")
            ) {
                Icon(
                    imageVector = if (speechState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (speechState.isListening) "Stop Speech Recognition" else "Start Speech Recognition",
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Text(
            text = if (speechState.isListening)
                "Listening in real-time... Tap to finish"
            else
                "Tap to start real-time speech input",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (speechState.isListening) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Sample Voice Prompts to try
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Try Saying:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val samplePrompts = listOf(
                "💬 WhatsApp kholo" to "Hindi/Hinglish",
                "📞 Call Mummy" to "Bilingual calling",
                "📺 Open YouTube and search Bollywood songs" to "App launch",
                "⚙️ Open Android Settings" to "System action",
                "🇮🇳 Arushi namaste, aaj ka mausam kaisa hai?" to "Natural Hindi prompt"
            )

            samplePrompts.forEach { (prompt, desc) ->
                Surface(
                    onClick = {
                        onStartListening(speechState.selectedLanguage, speechState.continuousMode)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun borderFromState(isListening: Boolean): androidx.compose.foundation.BorderStroke? {
    return if (isListening) {
        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF))
    } else {
        null
    }
}
