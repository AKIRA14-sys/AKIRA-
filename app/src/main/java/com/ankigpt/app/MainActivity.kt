package com.ankigpt.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.ankigpt.app.data.*
import com.ankigpt.app.data.tts.AnkiTtsManager
import com.ankigpt.app.ui.components.GlowBackground
import com.ankigpt.app.ui.screens.*
import com.ankigpt.app.ui.theme.*

class MainActivity : FragmentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var aiProviderManager: AiProviderManager
    private lateinit var webSearchService: WebSearchService
    private lateinit var fileAccessService: FileAccessService
    private lateinit var deviceControlManager: DeviceControlManager
    private lateinit var appResolver: AnkiAppResolver
    private lateinit var commandRouter: AnkiCommandRouter
    private lateinit var ankiTtsManager: AnkiTtsManager
    private lateinit var notificationManager: AnkiNotificationManager
    private lateinit var securityManager: AnkiSecurityManager
    private lateinit var historyRepository: HistoryRepository
    private lateinit var memoryRepository: MemoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)
        aiProviderManager = AiProviderManager()
        webSearchService = WebSearchService()
        fileAccessService = FileAccessService(applicationContext)
        deviceControlManager = DeviceControlManager(applicationContext)
        appResolver = AnkiAppResolver(applicationContext)
        commandRouter = AnkiCommandRouter(appResolver, deviceControlManager)
        ankiTtsManager = AnkiTtsManager(applicationContext, deviceControlManager)
        notificationManager = AnkiNotificationManager(applicationContext)
        securityManager = AnkiSecurityManager(applicationContext)
        historyRepository = HistoryRepository(applicationContext)
        memoryRepository = MemoryRepository(applicationContext)

        setContent {
            AnkiGptTheme {
                AnkiGptAppMain(
                    activity = this@MainActivity,
                    settingsRepository = settingsRepository,
                    aiProviderManager = aiProviderManager,
                    webSearchService = webSearchService,
                    fileAccessService = fileAccessService,
                    deviceControlManager = deviceControlManager,
                    commandRouter = commandRouter,
                    ankiTtsManager = ankiTtsManager,
                    securityManager = securityManager,
                    historyRepository = historyRepository,
                    memoryRepository = memoryRepository,
                    notificationManager = notificationManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceControlManager.shutdown()
        ankiTtsManager.stop()
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
    activity: FragmentActivity,
    settingsRepository: SettingsRepository,
    aiProviderManager: AiProviderManager,
    webSearchService: WebSearchService,
    fileAccessService: FileAccessService,
    deviceControlManager: DeviceControlManager,
    commandRouter: AnkiCommandRouter,
    ankiTtsManager: AnkiTtsManager,
    securityManager: AnkiSecurityManager,
    historyRepository: HistoryRepository,
    memoryRepository: MemoryRepository,
    notificationManager: AnkiNotificationManager
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CHAT) }
    var codeStudioSnippet by remember { mutableStateOf("") }
    val isAppLockEnabled by settingsRepository.appLockEnabledFlow.collectAsState(initial = false)
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(isAppLockEnabled) {
        if (isAppLockEnabled && !isUnlocked) {
            securityManager.authenticate(
                activity = activity,
                onSuccess = { isUnlocked = true },
                onError = { isUnlocked = false }
            )
        } else {
            isUnlocked = true
        }
    }

    GlowBackground {
        if (isAppLockEnabled && !isUnlocked) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkBackground
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = NeonCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AnkiGPT Locked",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            securityManager.authenticate(
                                activity = activity,
                                onSuccess = { isUnlocked = true },
                                onError = { isUnlocked = false }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text(text = "Unlock with Biometrics / PIN", color = DarkBackground)
                    }
                }
            }
        } else {
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
                            aiProviderManager = aiProviderManager,
                            webSearchService = webSearchService,
                            fileAccessService = fileAccessService,
                            deviceControlManager = deviceControlManager,
                            commandRouter = commandRouter,
                            ankiTtsManager = ankiTtsManager,
                            historyRepository = historyRepository,
                            memoryRepository = memoryRepository,
                            notificationManager = notificationManager,
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
                            aiProviderManager = aiProviderManager,
                            deviceControlManager = deviceControlManager,
                            ankiTtsManager = ankiTtsManager
                        )
                    }
                }
            }
        }
    }
}
