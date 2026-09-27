package com.ankigpt.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ankigpt.app.data.DeviceControlManager
import com.ankigpt.app.data.SystemStatus
import com.ankigpt.app.ui.theme.*

@Composable
fun DeviceHubScreen(deviceControlManager: DeviceControlManager) {
    var systemStatus by remember { mutableStateOf<SystemStatus?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        systemStatus = deviceControlManager.getSystemStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "🛠️ DEVICE CONTROL HUB",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Hardware diagnostics, flashlight & system controls",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System Diagnostic Metrics
        systemStatus?.let { status ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    value = "${status.batteryPercentage}%",
                    subValue = if (status.isCharging) "⚡ Charging" else "Discharging",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (status.batteryPercentage > 20) NeonGreen else NeonPink
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Storage Free",
                    value = "${status.availableStorageMB / 1024} GB",
                    subValue = "Total ${status.totalStorageMB / 1024} GB",
                    icon = Icons.Default.Storage,
                    accentColor = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                color = GlassSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Specs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Model: ${status.deviceModel}", fontSize = 13.sp, color = TextPrimary)
                    Text(text = "OS: ${status.androidVersion}", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hardware Controls",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Flashlight Toggle
        ControlTile(
            title = "Torch / Flashlight",
            subtitle = if (isFlashlightOn) "Torch ON" else "Torch OFF",
            icon = Icons.Default.FlashOn,
            isActive = isFlashlightOn,
            onClick = {
                isFlashlightOn = !isFlashlightOn
                statusMessage = deviceControlManager.toggleFlashlight(isFlashlightOn)
                deviceControlManager.triggerVibration(60)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Trigger Haptic Pulse
        ControlTile(
            title = "Haptic Vibration Pulse",
            subtitle = "Test motor feedback",
            icon = Icons.Default.Vibration,
            isActive = false,
            onClick = {
                deviceControlManager.triggerVibration(150)
                statusMessage = "Haptic pulse triggered!"
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Read Clipboard Content
        ControlTile(
            title = "Inspect Clipboard",
            subtitle = "Read system clipboard text",
            icon = Icons.Default.ContentPaste,
            isActive = false,
            onClick = {
                val clipText = deviceControlManager.getClipboardText()
                statusMessage = if (clipText.isNotBlank()) "Clipboard: \"$clipText\"" else "Clipboard is empty."
            }
        )

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                color = GlassSurface
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        color = GlassSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, fontSize = 12.sp, color = TextSecondary)
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subValue, fontSize = 11.sp, color = accentColor)
        }
    }
}

@Composable
fun ControlTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isActive) NeonCyan else GlassBorder,
                RoundedCornerShape(16.dp)
            ),
        color = if (isActive) DarkSurface else GlassSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) NeonCyan else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}
