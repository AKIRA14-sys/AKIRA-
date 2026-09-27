package com.ankigpt.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankigpt.app.data.DeviceControlManager
import com.ankigpt.app.data.SettingsRepository
import com.ankigpt.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    deviceControlManager: DeviceControlManager
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val currentApiKey by settingsRepository.apiKeyFlow.collectAsState(initial = "")
    val currentBaseUrl by settingsRepository.baseUrlFlow.collectAsState(initial = SettingsRepository.DEFAULT_BASE_URL)
    val currentModel by settingsRepository.selectedModelFlow.collectAsState(initial = SettingsRepository.DEFAULT_MODEL)
    val currentSystemPrompt by settingsRepository.systemPromptFlow.collectAsState(initial = SettingsRepository.DEFAULT_SYSTEM_PROMPT)

    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var systemPromptInput by remember { mutableStateOf("") }
    var savedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(currentApiKey, currentBaseUrl, currentModel, currentSystemPrompt) {
        apiKeyInput = currentApiKey
        baseUrlInput = currentBaseUrl
        modelInput = currentModel
        systemPromptInput = currentSystemPrompt
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "⚙️ ROUTER & SYSTEM CONFIG",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Configure Gonka Router or OpenRouter key & AI parameters",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gonka API Key Input
        Text(text = "Gonka / OpenRouter API Key", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            placeholder = { Text("Enter Gonka Router API Key", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // API Base URL Input
        Text(text = "API Base Endpoint URL", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = baseUrlInput,
            onValueChange = { baseUrlInput = it },
            placeholder = { Text(SettingsRepository.DEFAULT_BASE_URL, color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // AI Model Name Input
        Text(text = "Target AI Model", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            placeholder = { Text(SettingsRepository.DEFAULT_MODEL, color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // System Prompt Input
        Text(text = "System Prompt Persona", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = systemPromptInput,
            onValueChange = { systemPromptInput = it },
            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    settingsRepository.saveApiKey(apiKeyInput.trim())
                    settingsRepository.saveBaseUrl(baseUrlInput.trim())
                    settingsRepository.saveSelectedModel(modelInput.trim())
                    settingsRepository.saveSystemPrompt(systemPromptInput.trim())

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
                text = "SAVE CONFIGURATION",
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
                    text = "✓ Router settings successfully updated!",
                    modifier = Modifier.padding(12.dp),
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
