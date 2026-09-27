package com.ankigpt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ankigpt.app.data.*
import com.ankigpt.app.ui.components.GlowBackground
import com.ankigpt.app.ui.screens.*
import com.ankigpt.app.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var routerApiService: RouterApiService
    private lateinit var webSearchService: WebSearchService
    private lateinit var fileAccessService: FileAccessService
    private lateinit var deviceControlManager: DeviceControlManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)
        routerApiService = RouterApiService()
        webSearchService = WebSearchService()
        fileAccessService = FileAccessService(applicationContext)
        deviceControlManager = DeviceControlManager(applicationContext)

        setContent {
            AnkiGptTheme {
                AnkiGptAppMain(
                    settingsRepository = settingsRepository,
                    routerApiService = routerApiService,
                    webSearchService = webSearchService,
                    fileAccessService = fileAccessService,
                    deviceControlManager = deviceControlManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceControlManager.shutdown()
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    CHAT("Chat", Icons.Default.Chat),
    CODE("Code", Icons.Default.Code),
    DEVICE("Device", Icons.Default.Smartphone),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AnkiGptAppMain(
    settingsRepository: SettingsRepository,
    routerApiService: RouterApiService,
    webSearchService: WebSearchService,
    fileAccessService: FileAccessService,
    deviceControlManager: DeviceControlManager
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CHAT) }
    var codeStudioSnippet by remember { mutableStateOf("") }

    GlowBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = GlassSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    NavigationTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                deviceControlManager.triggerVibration(25)
                                selectedTab = tab
                            },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(text = tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBackground,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    NavigationTab.CHAT -> ChatScreen(
                        settingsRepository = settingsRepository,
                        routerApiService = routerApiService,
                        webSearchService = webSearchService,
                        fileAccessService = fileAccessService,
                        deviceControlManager = deviceControlManager,
                        onNavigateToCodeStudio = { snippet ->
                            codeStudioSnippet = snippet
                            selectedTab = NavigationTab.CODE
                        }
                    )
                    NavigationTab.CODE -> CodeStudioScreen(
                        initialCodeSnippet = codeStudioSnippet,
                        deviceControlManager = deviceControlManager
                    )
                    NavigationTab.DEVICE -> DeviceHubScreen(
                        deviceControlManager = deviceControlManager
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(
                        settingsRepository = settingsRepository,
                        deviceControlManager = deviceControlManager
                    )
                }
            }
        }
    }
}
