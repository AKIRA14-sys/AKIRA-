package com.ankigpt.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankigpt.app.R
import com.ankigpt.app.data.*
import com.ankigpt.app.ui.theme.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    settingsRepository: SettingsRepository,
    routerApiService: RouterApiService,
    webSearchService: WebSearchService,
    fileAccessService: FileAccessService,
    deviceControlManager: DeviceControlManager,
    onNavigateToCodeStudio: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val apiKey by settingsRepository.apiKeyFlow.collectAsState(initial = "")
    val baseUrl by settingsRepository.baseUrlFlow.collectAsState(initial = SettingsRepository.DEFAULT_BASE_URL)
    val model by settingsRepository.selectedModelFlow.collectAsState(initial = SettingsRepository.DEFAULT_MODEL)
    val systemPrompt by settingsRepository.systemPromptFlow.collectAsState(initial = SettingsRepository.DEFAULT_SYSTEM_PROMPT)

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isWebSearchActive by remember { mutableStateOf(false) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedFileContent by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val (fileName, content) = fileAccessService.readFileContentFromUri(it)
                attachedFileName = fileName
                attachedFileContent = content
                deviceControlManager.triggerVibration(50)
            }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank() && attachedFileContent == null) return

        val userPrompt = buildString {
            if (attachedFileName != null && attachedFileContent != null) {
                append("[ATTACHED FILE: $attachedFileName]\n```\n$attachedFileContent\n```\n\n")
            }
            append(inputText.trim())
        }

        val newMessages = messages.toMutableList().apply {
            add(ChatMessage("user", userPrompt))
        }
        messages = newMessages

        val currentInput = inputText
        inputText = ""
        attachedFileName = null
        attachedFileContent = null
        isGenerating = true
        deviceControlManager.triggerVibration(40)

        scope.launch {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))

            var fullUserPrompt = userPrompt
            if (isWebSearchActive) {
                val searchResults = webSearchService.searchWeb(currentInput)
                val searchSummary = searchResults.joinToString("\n\n") {
                    "Title: ${it.title}\nURL: ${it.url}\nSnippet: ${it.snippet}"
                }
                fullUserPrompt = "Context from Web Search:\n$searchSummary\n\nUser Question: $userPrompt"
            }

            val apiMessages = newMessages.dropLast(1) + ChatMessage("user", fullUserPrompt)
            val assistantIndex = messages.size
            messages = messages + ChatMessage("assistant", "⚡ Processing AnkiGPT Response...")

            var assistantResponse = ""

            routerApiService.streamChatCompletion(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                systemPrompt = systemPrompt,
                messages = apiMessages
            ).catch { e ->
                isGenerating = false
                val errorText = "\n⚠️ Error: ${e.localizedMessage ?: "Connection failed. Please verify API Key in Settings."}"
                messages = messages.toMutableList().apply {
                    if (size > assistantIndex) {
                        this[assistantIndex] = ChatMessage("assistant", assistantResponse + errorText)
                    }
                }
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
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // App Header Banner with Official AnkiGPT Logo
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
                            text = if (apiKey.isBlank()) "Gonka Router Mode" else "Model: $model",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isGenerating) NeonPink else NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = { isWebSearchActive = !isWebSearchActive }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Web Search",
                            tint = if (isWebSearchActive) NeonCyan else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_ankigpt_logo),
                                contentDescription = "AnkiGPT Central Emblem",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "AnkiGPT Personal AI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ask coding questions, search the web, analyze files, or control hardware.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                            )
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
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .border(
                                1.dp,
                                if (isUser) NeonCyan.copy(alpha = 0.5f) else GlassBorder,
                                RoundedCornerShape(16.dp)
                            ),
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
                                        IconButton(
                                            onClick = { deviceControlManager.speakText(msg.content) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Read Aloud",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (isCode) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onNavigateToCodeStudio(msg.content) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Code,
                                                    contentDescription = "Code Studio",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
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

        // Attached File Indicator
        attachedFileName?.let { name ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = DarkSurface
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name, fontSize = 12.sp, color = TextPrimary)
                    }
                    IconButton(onClick = {
                        attachedFileName = null
                        attachedFileContent = null
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove File", tint = NeonPink)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Controls & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GlassSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach File",
                    tint = NeonCyan
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask AnkiGPT...", color = TextSecondary) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurface,
                    unfocusedContainerColor = GlassSurface
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage() },
                enabled = !isGenerating && (inputText.isNotBlank() || attachedFileContent != null),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isGenerating) Brush.linearGradient(listOf(GlassSurface, GlassSurface)) else Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = DarkBackground
                )
            }
        }
    }
}
