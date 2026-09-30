package com.ankigpt.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ankigpt.app.R
import com.ankigpt.app.data.*
import com.ankigpt.app.data.tts.AnkiTtsManager
import com.ankigpt.app.service.AnkiOverlayService
import com.ankigpt.app.ui.theme.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    settingsRepository: SettingsRepository,
    aiProviderManager: AiProviderManager,
    webSearchService: WebSearchService,
    fileAccessService: FileAccessService,
    deviceControlManager: DeviceControlManager,
    commandRouter: AnkiCommandRouter,
    ankiTtsManager: AnkiTtsManager,
    historyRepository: HistoryRepository,
    memoryRepository: MemoryRepository,
    notificationManager: AnkiNotificationManager,
    onNavigateToCodeStudio: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val selectedProviderStr by settingsRepository.selectedProviderFlow.collectAsState(initial = "OPENROUTER")
    val openRouterKey by settingsRepository.openRouterApiKeyFlow.collectAsState(initial = "")
    val groqKey by settingsRepository.groqApiKeyFlow.collectAsState(initial = "")
    val geminiKey by settingsRepository.geminiApiKeyFlow.collectAsState(initial = "")

    val openRouterModel by settingsRepository.openRouterModelFlow.collectAsState(initial = "deepseek/deepseek-r1:free")
    val groqModel by settingsRepository.groqModelFlow.collectAsState(initial = "llama-3.3-70b-versatile")
    val geminiModel by settingsRepository.geminiModelFlow.collectAsState(initial = "gemini-1.5-flash")

    val systemPrompt by settingsRepository.systemPromptFlow.collectAsState(initial = SettingsRepository.DEFAULT_SYSTEM_PROMPT)
    val ttsProviderType by settingsRepository.ttsProviderFlow.collectAsState(initial = SettingsRepository.DEFAULT_TTS_PROVIDER)
    val elevenLabsApiKey by settingsRepository.elevenLabsApiKeyFlow.collectAsState(initial = "")
    val elevenLabsVoiceId by settingsRepository.elevenLabsVoiceIdFlow.collectAsState(initial = "21m00Tcm4TlvDq8ikWAM")

    val savedConversations by historyRepository.conversationsFlow.collectAsState(initial = emptyList())
    val savedMemories by memoryRepository.memoriesFlow.collectAsState(initial = emptyList())

    val providerType = try { AiProviderType.valueOf(selectedProviderStr) } catch (_: Exception) { AiProviderType.OPENROUTER }
    val apiKey = when (providerType) {
        AiProviderType.OPENROUTER -> openRouterKey
        AiProviderType.GROQ -> groqKey
        AiProviderType.GEMINI -> geminiKey
    }
    val model = when (providerType) {
        AiProviderType.OPENROUTER -> openRouterModel
        AiProviderType.GROQ -> groqModel
        AiProviderType.GEMINI -> geminiModel
    }

    var currentConversationId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var isWebSearchActive by remember { mutableStateOf(false) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileContent by remember { mutableStateOf<String?>(null) }
    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showMemoryDrawer by remember { mutableStateOf(false) }
    var newMemoryInput by remember { mutableStateOf("") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                deviceControlManager.triggerVibration(40)
            }
        }
    }

    fun startNativeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition unavailable on this device.", Toast.LENGTH_SHORT).show()
            isListeningVoice = false
            return
        }

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningVoice = false
                }
                override fun onError(error: Int) {
                    isListeningVoice = false
                    try { recognizer.destroy() } catch (_: Exception) {}
                    Toast.makeText(context, "Voice input error ($error). Try again.", Toast.LENGTH_SHORT).show()
                }
                override fun onResults(results: Bundle?) {
                    isListeningVoice = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()
                    if (!spoken.isNullOrBlank()) {
                        inputText = spoken
                        deviceControlManager.triggerVibration(40)
                    }
                    try { recognizer.destroy() } catch (_: Exception) {}
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            isListeningVoice = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListeningVoice = false
            Toast.makeText(context, "Microphone error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening... Speak to AnkiGPT")
                }
                isListeningVoice = true
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                startNativeSpeechRecognizer()
            }
        } else {
            deviceControlManager.triggerVibration(100)
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening... Speak to AnkiGPT")
                }
                isListeningVoice = true
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                startNativeSpeechRecognizer()
            }
        } else {
            try {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not request audio permission.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // OpenDocument file picker contract (supported across all Android devices)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val (fileName, content) = fileAccessService.readFileContentFromUri(it)
                    attachedFileName = fileName
                    attachedFileContent = content
                    deviceControlManager.triggerVibration(50)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error selecting file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fallback GetContent file picker
    val getContentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val (fileName, content) = fileAccessService.readFileContentFromUri(it)
                    attachedFileName = fileName
                    attachedFileContent = content
                    deviceControlManager.triggerVibration(50)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error selecting file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun safeLaunchFilePicker() {
        try {
            documentPickerLauncher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            try {
                getContentPickerLauncher.launch("*/*")
            } catch (e2: Exception) {
                Toast.makeText(context, "No compatible file picker found on device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Capture Launcher
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedImageBitmap = bitmap
            attachedFileName = "camera_captured_photo.jpg"
            attachedFileContent = "[Captured Image Attached for AI Analysis]"
            deviceControlManager.triggerVibration(60)
            Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun safeLaunchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open camera.", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not request camera permission.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun speakResponse(text: String) {
        scope.launch {
            ankiTtsManager.speak(
                text = text,
                selectedProviderType = ttsProviderType,
                geminiApiKey = geminiKey,
                elevenLabsApiKey = elevenLabsApiKey,
                elevenLabsVoiceId = elevenLabsVoiceId
            )
        }
    }

    fun toggleOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Grant 'Display over other apps' in Settings", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                val intent = Intent(context, AnkiOverlayService::class.java)
                context.startService(intent)
                Toast.makeText(context, "Hey Anki Overlay Activated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to start overlay: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun processUserInput(promptText: String) {
        if (promptText.isBlank() && attachedFileContent == null && capturedImageBitmap == null) return

        val userPrompt = buildString {
            if (attachedFileName != null && attachedFileContent != null) {
                append("[ATTACHED FILE/IMAGE: $attachedFileName]\n```\n$attachedFileContent\n```\n\n")
            }
            append(promptText.trim())
        }

        val newMessages = messages.toMutableList().apply {
            add(ChatMessage("user", userPrompt))
        }
        messages = newMessages

        val currentInput = promptText
        inputText = ""
        attachedFileName = null
        attachedFileContent = null
        capturedImageBitmap = null
        deviceControlManager.triggerVibration(40)

        // Command Router
        val routeResult = commandRouter.processCommand(currentInput)

        when (routeResult) {
            is CommandResult.LocalAction -> {
                val localMsg = ChatMessage("assistant", "⚡ [Local Action] ${routeResult.responseText}")
                messages = messages + localMsg
                speakResponse(routeResult.responseText)
                scope.launch {
                    historyRepository.saveConversation(
                        ChatConversation(
                            id = currentConversationId,
                            title = newMessages.firstOrNull()?.content?.take(30) ?: "Chat",
                            messages = messages
                        )
                    )
                }
            }
            is CommandResult.AiRequest -> {
                isGenerating = true
                scope.launch {
                    listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))

                    val memoryContext = if (savedMemories.isNotEmpty()) {
                        "User Saved Memories:\n" + savedMemories.joinToString("\n") { "- ${it.content}" } + "\n\n"
                    } else ""

                    var fullUserPrompt = memoryContext + userPrompt
                    if (isWebSearchActive) {
                        val searchResults = webSearchService.searchWeb(currentInput)
                        val searchSummary = searchResults.joinToString("\n\n") {
                            "Title: ${it.title}\nURL: ${it.url}\nSnippet: ${it.snippet}"
                        }
                        fullUserPrompt += "\n\nContext from Web Search:\n$searchSummary"
                    }

                    val apiMessages = newMessages.dropLast(1) + ChatMessage("user", fullUserPrompt)
                    val assistantIndex = messages.size
                    messages = messages + ChatMessage("assistant", "⚡ Processing AnkiGPT Response...")

                    var assistantResponse = ""

                    aiProviderManager.streamChatCompletion(
                        provider = providerType,
                        apiKey = apiKey,
                        model = model,
                        systemPrompt = systemPrompt,
                        messages = apiMessages
                    ).catch { e ->
                        isGenerating = false
                        val errorText = "\n⚠️ Error: ${e.localizedMessage ?: "Connection failed."}"
                        messages = messages.toMutableList().apply {
                            if (size > assistantIndex) {
                                this[assistantIndex] = ChatMessage("assistant", assistantResponse + errorText)
                            }
                        }
                        notificationManager.showTaskCompletionNotification("AnkiGPT Error", "Task failed: ${e.localizedMessage}")
                    }.collect { chunk ->
                        assistantResponse += chunk
                        messages = messages.toMutableList().apply {
                            if (size > assistantIndex) {
                                this[assistantIndex] = ChatMessage("assistant", assistantResponse)
                            }
                        }
                        listState.animateScrollToItem(assistantIndex)
                    }

                    isGenerating = false
                    historyRepository.saveConversation(
                        ChatConversation(
                            id = currentConversationId,
                            title = newMessages.firstOrNull()?.content?.take(30) ?: "Chat",
                            messages = messages
                        )
                    )
                    notificationManager.showTaskCompletionNotification("AnkiGPT Completed", "AI response generation complete.")

                    if (assistantResponse.isNotBlank()) {
                        speakResponse(assistantResponse.take(300))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // App Header Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
            color = GlassSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ankigpt_logo),
                        contentDescription = "AnkiGPT Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AnkiGPT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${providerType.displayName} ($model)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showHistoryDrawer = !showHistoryDrawer }) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = NeonCyan)
                    }
                    IconButton(onClick = { showMemoryDrawer = !showMemoryDrawer }) {
                        Icon(imageVector = Icons.Default.Psychology, contentDescription = "Memory", tint = NeonPurple)
                    }
                    IconButton(onClick = { isWebSearchActive = !isWebSearchActive }) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Web Search",
                            tint = if (isWebSearchActive) NeonCyan else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History Drawer Overlay
        if (showHistoryDrawer) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Chat History", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 14.sp)
                        TextButton(onClick = {
                            currentConversationId = java.util.UUID.randomUUID().toString()
                            messages = emptyList()
                            showHistoryDrawer = false
                        }) {
                            Text("+ New Chat", color = NeonGreen, fontSize = 12.sp)
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(savedConversations) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentConversationId = c.id
                                        messages = c.messages
                                        showHistoryDrawer = false
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = c.title, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
                                IconButton(
                                    onClick = { scope.launch { historyRepository.deleteConversation(c.id) } },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = NeonPink, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Memory Drawer Overlay
        if (showMemoryDrawer) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                color = DarkSurface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Saved Memories", fontWeight = FontWeight.Bold, color = NeonPurple, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newMemoryInput,
                            onValueChange = { newMemoryInput = it },
                            placeholder = { Text("Add memory (e.g. I prefer Kotlin)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(onClick = {
                            if (newMemoryInput.isNotBlank()) {
                                scope.launch {
                                    memoryRepository.addMemory(newMemoryInput)
                                    newMemoryInput = ""
                                }
                            }
                        }) {
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(savedMemories) { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "• ${m.content}", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { scope.launch { memoryRepository.deleteMemory(m.id) } }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = NeonPink, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Attached File/Image Preview Bar
        if (attachedFileName != null || capturedImageBitmap != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                color = GlassSurface
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (capturedImageBitmap != null) {
                            Image(
                                bitmap = capturedImageBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = attachedFileName ?: "Attached Image", fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                    }
                    IconButton(
                        onClick = {
                            attachedFileName = null
                            attachedFileContent = null
                            capturedImageBitmap = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = NeonPink)
                    }
                }
            }
        }

        // Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_ankigpt_logo),
                                contentDescription = "AnkiGPT Emblem",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "AnkiGPT Personal AI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "Try: \"open whatsapp\", \"hello anki\", \"what's the time\", or ask any question.",
                                fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Open Assistant Settings in Android Settings", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                ) {
                                    Text("Default Assistant", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { toggleOverlayService() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple)
                                ) {
                                    Text("Display Over Apps", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            items(messages) { msg ->
                val isUser = msg.role == "user"
                val isCode = msg.content.contains("```")

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp))
                            .border(1.dp, if (isUser) NeonCyan.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(16.dp)),
                        color = if (isUser) DarkSurface else GlassSurface
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isUser) "YOU" else "AnkiGPT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) NeonCyan else NeonPurple
                                )

                                if (!isUser) {
                                    Row {
                                        IconButton(onClick = { speakResponse(msg.content) }, modifier = Modifier.size(24.dp)) {
                                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                        if (isCode) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(onClick = { onNavigateToCodeStudio(msg.content) }, modifier = Modifier.size(24.dp)) {
                                                Icon(imageVector = Icons.Default.Code, contentDescription = "Code Studio", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg.content,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        // Input Controls & Microphone
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { safeLaunchFilePicker() },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(GlassSurface)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Attach File", tint = NeonCyan)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { safeLaunchCamera() },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(GlassSurface)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Capture Camera Photo", tint = NeonGreen)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { startVoiceInput() },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isListeningVoice) NeonPink else GlassSurface)
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Input", tint = if (isListeningVoice) DarkBackground else NeonPink)
            }

            Spacer(modifier = Modifier.width(4.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(if (isListeningVoice) "Listening..." else "Ask AnkiGPT...", color = TextSecondary, fontSize = 12.sp) },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { processUserInput(inputText) },
                enabled = !isGenerating && (inputText.isNotBlank() || attachedFileContent != null || capturedImageBitmap != null),
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    brush = if (isGenerating) Brush.linearGradient(listOf(GlassSurface, GlassSurface)) else Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                )
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = DarkBackground)
            }
        }
    }
}
