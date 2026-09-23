package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.model.ConnectionStatus
import com.example.ui.components.DeviceLogsScreen
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.TranscriptView
import com.example.ui.components.WebBridgePlayground
import com.example.viewmodel.ArushiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArushiApp(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val inputVolume by viewModel.inputVolume.collectAsState()
    val outputVolume by viewModel.outputVolume.collectAsState()
    val currentStreamingText by viewModel.currentStreamingText.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val actionHistory by viewModel.actionHistory.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startListening()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFE040FB), Color(0xFF00E5FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Arushi Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Arushi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            when (connectionStatus) {
                                                ConnectionStatus.CONNECTED -> Color(0xFF10B981)
                                                ConnectionStatus.CONNECTING -> Color(0xFFF59E0B)
                                                ConnectionStatus.ERROR -> Color(0xFFEF4444)
                                                ConnectionStatus.DISCONNECTED -> Color(0xFF6B7280)
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when (connectionStatus) {
                                        ConnectionStatus.CONNECTED -> "Live Connected"
                                        ConnectionStatus.CONNECTING -> "Connecting..."
                                        ConnectionStatus.ERROR -> "Connection Error"
                                        ConnectionStatus.DISCONNECTED -> "Tap Mic to Start"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Detected language badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = detectedLanguage,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.connect() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconnect",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Live Assistant") },
                    label = { Text("Assistant") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Device Bridge") },
                    label = { Text("Device Bridge") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Web Bridge") },
                    label = { Text("Web Bridge") }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> LiveAssistantView(
                    assistantState = assistantState,
                    inputVolume = inputVolume,
                    outputVolume = outputVolume,
                    messages = messages,
                    currentStreamingText = currentStreamingText,
                    onQuickCommand = { cmd -> viewModel.sendTextCommand(cmd) },
                    onToggleMic = {
                        if (hasMicPermission) {
                            viewModel.toggleListening()
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> DeviceLogsScreen(
                    actionHistory = actionHistory,
                    onOpenWhatsApp = { viewModel.actionManager.openWhatsApp() },
                    onOpenYouTube = { viewModel.actionManager.openApp("YouTube") },
                    onOpenSettings = { viewModel.actionManager.openApp("Settings") },
                    onCallTestNumber = { viewModel.actionManager.makeCall("9876543210") },
                    onCallContactMom = { viewModel.actionManager.callContact("Mom") },
                    onCallContactRahul = { viewModel.actionManager.callContact("Rahul") },
                    onClearHistory = { viewModel.clearHistory() },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> WebBridgePlayground(
                    androidBridge = viewModel.androidBridge,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showSettingsDialog) {
        VoiceSettingsDialog(
            selectedVoice = selectedVoice,
            onSelectVoice = { voice -> viewModel.setVoice(voice) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun LiveAssistantView(
    assistantState: AssistantState,
    inputVolume: Float,
    outputVolume: Float,
    messages: List<com.example.model.ChatMessage>,
    currentStreamingText: String,
    onQuickCommand: (String) -> Unit,
    onToggleMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Section: Animated Reactive Orb
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            OrbVisualizer(
                assistantState = assistantState,
                inputVolume = inputVolume,
                outputVolume = outputVolume,
                size = 180.dp
            )
        }

        // State indicator text
        Text(
            text = when (assistantState) {
                AssistantState.LISTENING -> "Arushi is listening to you..."
                AssistantState.SPEAKING -> "Arushi is speaking..."
                AssistantState.THINKING -> "Thinking..."
                AssistantState.EXECUTING_ACTION -> "Executing phone action..."
                AssistantState.ERROR -> "Unable to connect. Tap reconnect."
                AssistantState.IDLE -> "Tap microphone or ask anything"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (assistantState) {
                AssistantState.LISTENING -> Color(0xFF00E5FF)
                AssistantState.SPEAKING -> Color(0xFFFF007F)
                AssistantState.THINKING -> Color(0xFFFFD600)
                AssistantState.EXECUTING_ACTION -> Color(0xFFFF9100)
                AssistantState.ERROR -> Color(0xFFFF5252)
                AssistantState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Quick Command Voice Chips (Hindi, English, Hinglish, Calling, Apps)
        QuickChipsRow(onSelectCommand = onQuickCommand)

        Spacer(modifier = Modifier.height(8.dp))

        // Middle Section: Transcript of Conversation & Actions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && currentStreamingText.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Namaste! I'm Arushi 🙏",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Speak in Hindi, English, Hinglish, Marathi, Bengali, Tamil, etc. I can open WhatsApp, make calls, search contacts, and launch apps!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                TranscriptView(
                    messages = messages,
                    currentStreamingText = currentStreamingText,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom Controls: Mic FAB with Pulse Animation
        BottomMicControl(
            isListening = assistantState == AssistantState.LISTENING,
            onToggleMic = onToggleMic,
            onSendCommand = onQuickCommand
        )
    }
}

@Composable
fun QuickChipsRow(onSelectCommand: (String) -> Unit) {
    val scrollState = rememberScrollState()
    val chips = listOf(
        "WhatsApp kholo" to "💬",
        "Open YouTube" to "📺",
        "Call Mom" to "📞",
        "Rahul ko call karo" to "👤",
        "Call 9876543210" to "📱",
        "Hindi mein baat karo" to "🇮🇳",
        "Hinglish mein bolo" to "✨",
        "Open Settings" to "⚙️",
        "Open Instagram" to "📸"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((text, emoji) in chips) {
            Surface(
                onClick = { onSelectCommand(text) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.testTag("quick_chip_$text")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomMicControl(
    isListening: Boolean,
    onToggleMic: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var isKeyboardOpen by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = isKeyboardOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type prompt (e.g. WhatsApp kholo)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_command_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendCommand(textInput)
                                textInput = ""
                                isKeyboardOpen = false
                            }
                        },
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isKeyboardOpen = !isKeyboardOpen }) {
                    Text(if (isKeyboardOpen) "Hide Keyboard" else "Type Instead")
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                        )
                    }

                    FloatingActionButton(
                        onClick = onToggleMic,
                        shape = CircleShape,
                        containerColor = if (isListening) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isListening) Color(0xFF00363A) else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("mic_fab_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isListening) "Mute Microphone" else "Start Microphone",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Text(
                    text = if (isListening) "Live Mic" else "Muted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun VoiceSettingsDialog(
    selectedVoice: String,
    onSelectVoice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val voices = listOf(
        "Aoede" to "Warm, friendly Indian/Global feminine voice",
        "Kore" to "Calm, gentle tone",
        "Puck" to "Upbeat, energetic tone",
        "Charon" to "Deep, authoritative tone",
        "Fenrir" to "Warm, confident masculine tone"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arushi Voice Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select speech voice for Gemini Live output:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                for ((voice, desc) in voices) {
                    FilterChip(
                        selected = selectedVoice == voice,
                        onClick = {
                            onSelectVoice(voice)
                            onDismiss()
                        },
                        label = {
                            Column {
                                Text(voice, fontWeight = FontWeight.Bold)
                                Text(desc, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
