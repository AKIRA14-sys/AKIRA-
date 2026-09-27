package com.ankigpt.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import com.ankigpt.app.data.DeviceControlManager
import com.ankigpt.app.data.SettingsRepository
import com.ankigpt.app.data.tts.AnkiTtsManager
import com.ankigpt.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    deviceControlManager: DeviceControlManager,
    ankiTtsManager: AnkiTtsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentApiKey by settingsRepository.apiKeyFlow.collectAsState(initial = "")
    val currentBaseUrl by settingsRepository.baseUrlFlow.collectAsState(initial = SettingsRepository.DEFAULT_BASE_URL)
    val currentModel by settingsRepository.selectedModelFlow.collectAsState(initial = SettingsRepository.DEFAULT_MODEL)
    val currentSystemPrompt by settingsRepository.systemPromptFlow.collectAsState(initial = SettingsRepository.DEFAULT_SYSTEM_PROMPT)
    val currentTtsProvider by settingsRepository.ttsProviderFlow.collectAsState(initial = SettingsRepository.DEFAULT_TTS_PROVIDER)
    val currentGeminiKey by settingsRepository.geminiApiKeyFlow.collectAsState(initial = "")
    val currentElevenLabsKey by settingsRepository.elevenLabsApiKeyFlow.collectAsState(initial = "")
    val currentElevenLabsVoiceId by settingsRepository.elevenLabsVoiceIdFlow.collectAsState(initial = "21m00Tcm4TlvDq8ikWAM")

    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var systemPromptInput by remember { mutableStateOf("") }
    var selectedTtsProvider by remember { mutableStateOf("android") }
    var geminiKeyInput by remember { mutableStateOf("") }
    var elevenLabsKeyInput by remember { mutableStateOf("") }
    var elevenLabsVoiceIdInput by remember { mutableStateOf("21m00Tcm4TlvDq8ikWAM") }
    var savedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(currentApiKey, currentBaseUrl, currentModel, currentSystemPrompt, currentTtsProvider, currentGeminiKey, currentElevenLabsKey, currentElevenLabsVoiceId) {
        apiKeyInput = currentApiKey
        baseUrlInput = currentBaseUrl
        modelInput = currentModel
        systemPromptInput = currentSystemPrompt
        selectedTtsProvider = currentTtsProvider
        geminiKeyInput = currentGeminiKey
        elevenLabsKeyInput = currentElevenLabsKey
        elevenLabsVoiceIdInput = currentElevenLabsVoiceId
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
            text = "Configure Default Assistant role, TTS voice engines, and API keys",
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
                    text = "Android Digital Assistant",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Set AnkiGPT as your default phone assistant to invoke via home-button or gesture from any screen.",
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

        if (selectedTtsProvider == "gemini") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiKeyInput,
                onValueChange = { geminiKeyInput = it },
                placeholder = { Text("Gemini API Key for Cloud TTS", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
                ),
                singleLine = true
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
                placeholder = { Text("ElevenLabs Voice ID (e.g., 21m00Tcm4TlvDq8ikWAM)", color = TextSecondary) },
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

        Spacer(modifier = Modifier.height(16.dp))

        // GONKA / OPENROUTER API KEY CONFIG
        Text(text = "Gonka / OpenRouter API Key", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text("Enter Gonka Router API Key", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Target AI Model", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            placeholder = { Text(SettingsRepository.DEFAULT_MODEL, color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan, unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface, unfocusedContainerColor = GlassSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.saveApiKey(apiKeyInput.trim())
                    settingsRepository.saveBaseUrl(baseUrlInput.trim())
                    settingsRepository.saveSelectedModel(modelInput.trim())
                    settingsRepository.saveSystemPrompt(systemPromptInput.trim())
                    settingsRepository.saveTtsProvider(selectedTtsProvider)
                    settingsRepository.saveGeminiApiKey(geminiKeyInput.trim())
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
                    text = "✓ Assistant settings successfully saved!",
                    modifier = Modifier.padding(12.dp),
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
