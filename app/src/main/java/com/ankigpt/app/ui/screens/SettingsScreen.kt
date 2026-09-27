package com.ankigpt.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankigpt.app.data.AiProviderManager
import com.ankigpt.app.data.AiProviderType
import com.ankigpt.app.data.DeviceControlManager
import com.ankigpt.app.data.SettingsRepository
import com.ankigpt.app.data.tts.AnkiTtsManager
import com.ankigpt.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    aiProviderManager: AiProviderManager,
    deviceControlManager: DeviceControlManager,
    ankiTtsManager: AnkiTtsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentProviderStr by settingsRepository.selectedProviderFlow.collectAsState(initial = "OPENROUTER")
    val currentOpenRouterKey by settingsRepository.openRouterApiKeyFlow.collectAsState(initial = "")
    val currentGroqKey by settingsRepository.groqApiKeyFlow.collectAsState(initial = "")
    val currentGeminiKey by settingsRepository.geminiApiKeyFlow.collectAsState(initial = "")
    val currentOpenRouterModel by settingsRepository.openRouterModelFlow.collectAsState(initial = "deepseek/deepseek-r1:free")
    val currentGroqModel by settingsRepository.groqModelFlow.collectAsState(initial = "llama-3.3-70b-versatile")
    val currentGeminiModel by settingsRepository.geminiModelFlow.collectAsState(initial = "gemini-1.5-flash")
    val currentAppLockEnabled by settingsRepository.appLockEnabledFlow.collectAsState(initial = false)

    val currentTtsProvider by settingsRepository.ttsProviderFlow.collectAsState(initial = SettingsRepository.DEFAULT_TTS_PROVIDER)
    val currentElevenLabsKey by settingsRepository.elevenLabsApiKeyFlow.collectAsState(initial = "")
    val currentElevenLabsVoiceId by settingsRepository.elevenLabsVoiceIdFlow.collectAsState(initial = "21m00Tcm4TlvDq8ikWAM")

    var selectedProviderType by remember { mutableStateOf(AiProviderType.OPENROUTER) }
    var openRouterKeyInput by remember { mutableStateOf("") }
    var groqKeyInput by remember { mutableStateOf("") }
    var geminiKeyInput by remember { mutableStateOf("") }
    var openRouterModelInput by remember { mutableStateOf("") }
    var groqModelInput by remember { mutableStateOf("") }
    var geminiModelInput by remember { mutableStateOf("") }
    var appLockToggle by remember { mutableStateOf(false) }

    var selectedTtsProvider by remember { mutableStateOf("android") }
    var elevenLabsKeyInput by remember { mutableStateOf("") }
    var elevenLabsVoiceIdInput by remember { mutableStateOf("21m00Tcm4TlvDq8ikWAM") }
    var savedFeedback by remember { mutableStateOf(false) }

    var availableModels by remember { mutableStateOf(listOf<String>()) }
    var isFetchingModels by remember { mutableStateOf(false) }

    fun fetchModelsForCurrentProvider() {
        isFetchingModels = true
        scope.launch {
            val key = when (selectedProviderType) {
                AiProviderType.OPENROUTER -> openRouterKeyInput
                AiProviderType.GROQ -> groqKeyInput
                AiProviderType.GEMINI -> geminiKeyInput
            }
            availableModels = aiProviderManager.listAvailableModels(selectedProviderType, key)
            isFetchingModels = false
        }
    }

    LaunchedEffect(currentProviderStr, currentOpenRouterKey, currentGroqKey, currentGeminiKey, currentOpenRouterModel, currentGroqModel, currentGeminiModel, currentTtsProvider, currentElevenLabsKey, currentElevenLabsVoiceId, currentAppLockEnabled) {
        selectedProviderType = try { AiProviderType.valueOf(currentProviderStr) } catch (_: Exception) { AiProviderType.OPENROUTER }
        openRouterKeyInput = currentOpenRouterKey
        groqKeyInput = currentGroqKey
        geminiKeyInput = currentGeminiKey
        openRouterModelInput = currentOpenRouterModel
        groqModelInput = currentGroqModel
        geminiModelInput = currentGeminiModel
        selectedTtsProvider = currentTtsProvider
        elevenLabsKeyInput = currentElevenLabsKey
        elevenLabsVoiceIdInput = currentElevenLabsVoiceId
        appLockToggle = currentAppLockEnabled

        fetchModelsForCurrentProvider()
    }

    LaunchedEffect(selectedProviderType) {
        fetchModelsForCurrentProvider()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "⚙️ ASSISTANT & AI CONFIG",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Configure Default Assistant role, AI Providers (OpenRouter, Groq, Gemini), Security & App Lock",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DEFAULT ASSISTANT ROLE BUTTON
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
            color = GlassSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A-Anki Android Assistant",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set AnkiGPT as your default phone assistant to invoke A-Anki via home-button or gesture from any app.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            deviceControlManager.triggerVibration(100)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Smartphone, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Set AnkiGPT as Default Assistant", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECURITY & APP LOCK
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
            color = GlassSurface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Biometric App Lock", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Protect conversations with Fingerprint/PIN", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Switch(
                    checked = appLockToggle,
                    onCheckedChange = { appLockToggle = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI PROVIDER SELECTION & CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Active AI Provider", fontSize = 14.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
            IconButton(onClick = { fetchModelsForCurrentProvider() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Models", tint = NeonCyan)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AiProviderType.entries.forEach { p ->
                FilterChip(
                    selected = selectedProviderType == p,
                    onClick = { selectedProviderType = p },
                    label = { Text(p.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedProviderType) {
            AiProviderType.OPENROUTER -> ProviderCard(
                name = "OpenRouter",
                apiKey = openRouterKeyInput,
                onKeyChange = { openRouterKeyInput = it },
                model = openRouterModelInput,
                onModelChange = { openRouterModelInput = it },
                availableModels = availableModels,
                isFetching = isFetchingModels
            )
            AiProviderType.GROQ -> ProviderCard(
                name = "GROQ",
                apiKey = groqKeyInput,
                onKeyChange = { groqKeyInput = it },
                model = groqModelInput,
                onModelChange = { groqModelInput = it },
                availableModels = availableModels,
                isFetching = isFetchingModels
            )
            AiProviderType.GEMINI -> ProviderCard(
                name = "Google Gemini",
                apiKey = geminiKeyInput,
                onKeyChange = { geminiKeyInput = it },
                model = geminiModelInput,
                onModelChange = { geminiModelInput = it },
                availableModels = availableModels,
                isFetching = isFetchingModels
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // VOICE & TTS PROVIDER CONFIG
        Text(text = "Voice & Speech Engine (TTS)", fontSize = 14.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = selectedTtsProvider == "android",
                onClick = { selectedTtsProvider = "android" },
                label = { Text("Android TTS") }
            )
            FilterChip(
                selected = selectedTtsProvider == "gemini",
                onClick = { selectedTtsProvider = "gemini" },
                label = { Text("Gemini TTS") }
            )
            FilterChip(
                selected = selectedTtsProvider == "elevenlabs",
                onClick = { selectedTtsProvider = "elevenlabs" },
                label = { Text("ElevenLabs") }
            )
        }

        if (selectedTtsProvider == "elevenlabs") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = elevenLabsKeyInput,
                onValueChange = { elevenLabsKeyInput = it },
                placeholder = { Text("ElevenLabs API Key", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = elevenLabsVoiceIdInput,
                onValueChange = { elevenLabsVoiceIdInput = it },
                placeholder = { Text("ElevenLabs Voice ID", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    ankiTtsManager.speak(
                        text = "Hello! I am AnkiGPT, your personal AI assistant.",
                        selectedProviderType = selectedTtsProvider,
                        geminiApiKey = geminiKeyInput,
                        elevenLabsApiKey = elevenLabsKeyInput,
                        elevenLabsVoiceId = elevenLabsVoiceIdInput
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        ) {
            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Test Selected Voice", color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.saveSelectedProvider(selectedProviderType.name)
                    settingsRepository.saveOpenRouterApiKey(openRouterKeyInput.trim())
                    settingsRepository.saveGroqApiKey(groqKeyInput.trim())
                    settingsRepository.saveGeminiApiKey(geminiKeyInput.trim())
                    settingsRepository.saveOpenRouterModel(openRouterModelInput.trim())
                    settingsRepository.saveGroqModel(groqModelInput.trim())
                    settingsRepository.saveGeminiModel(geminiModelInput.trim())
                    settingsRepository.saveAppLockEnabled(appLockToggle)

                    settingsRepository.saveTtsProvider(selectedTtsProvider)
                    settingsRepository.saveElevenLabsApiKey(elevenLabsKeyInput.trim())
                    settingsRepository.saveElevenLabsVoiceId(elevenLabsVoiceIdInput.trim())

                    deviceControlManager.triggerVibration(80)
                    savedFeedback = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save Settings", tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SAVE ASSISTANT CONFIG",
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (savedFeedback) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NeonGreen, RoundedCornerShape(12.dp)),
                color = GlassSurface
            ) {
                Text(
                    text = "✓ Settings successfully updated!",
                    modifier = Modifier.padding(12.dp),
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderCard(
    name: String,
    apiKey: String,
    onKeyChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    availableModels: List<String>,
    isFetching: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        color = GlassSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "$name Credentials & Model", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onKeyChange,
                placeholder = { Text("$name API Key", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = onModelChange,
                    readOnly = false,
                    label = { Text("Selected Model", color = TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (isFetching) {
                        DropdownMenuItem(
                            text = { Text("Fetching models...") },
                            onClick = {}
                        )
                    } else {
                        availableModels.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    onModelChange(m)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
