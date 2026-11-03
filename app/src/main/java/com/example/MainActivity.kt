package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gateway.GatewayUiState
import com.example.ui.GatewayViewModel
import com.example.ui.screens.ApkAndSetupGuideScreen
import com.example.ui.screens.ConsoleLogsScreen
import com.example.ui.screens.GatewayDashboardScreen
import com.example.ui.screens.WebWorkspaceScreen
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

enum class AppDestination(
    val routeId: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    GATEWAY(
        routeId = "gateway_dashboard",
        labelResId = R.string.tab_gateway,
        selectedIcon = Icons.Filled.Router,
        unselectedIcon = Icons.Outlined.Router
    ),
    WORKSPACE(
        routeId = "web_workspace",
        labelResId = R.string.tab_workspace,
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code
    ),
    CONSOLE(
        routeId = "console_logs",
        labelResId = R.string.tab_logs,
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal
    ),
    APK_GUIDE(
        routeId = "apk_setup_guide",
        labelResId = R.string.tab_apk_guide,
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gatewayViewModel: GatewayViewModel = viewModel()
                val uiState by gatewayViewModel.uiState.collectAsStateWithLifecycle()

                AntigravityAppShell(
                    uiState = uiState,
                    onStartGateway = { gatewayViewModel.startGateway() },
                    onStopGateway = { gatewayViewModel.stopGateway() },
                    onRunStatusCheck = { gatewayViewModel.runStatusCheck() },
                    onRefreshCsrf = { gatewayViewModel.refreshCsrfTokenNow() },
                    onTriggerWatchdog = { gatewayViewModel.triggerManualWatchdogRevive() },
                    onSaveConfig = { proxyPort, targetHost, targetPort, csrfToken, autoRevive, holdWakeLock ->
                        gatewayViewModel.updateConfigAndRestart(
                            proxyPort = proxyPort,
                            targetHost = targetHost,
                            targetPort = targetPort,
                            csrfToken = csrfToken,
                            autoReviveWatchdog = autoRevive,
                            holdWakeLock = holdWakeLock
                        )
                    },
                    onExecuteCliCommand = { gatewayViewModel.executeCliCommand(it) },
                    onClearLogs = { gatewayViewModel.clearLogs() },
                    onToggleFullscreen = { gatewayViewModel.setFullscreenWorkspace(it) },
                    onToggleDesktopMode = { gatewayViewModel.toggleDesktopViewport() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityAppShell(
    uiState: GatewayUiState,
    onStartGateway: () -> Unit,
    onStopGateway: () -> Unit,
    onRunStatusCheck: () -> Unit,
    onRefreshCsrf: () -> Unit,
    onTriggerWatchdog: () -> Unit,
    onSaveConfig: (Int, String, Int, String, Boolean, Boolean) -> Unit,
    onExecuteCliCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onToggleDesktopMode: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(AppDestination.GATEWAY) }

    BackHandler(enabled = uiState.isFullscreenWorkspace || currentTab != AppDestination.GATEWAY) {
        if (uiState.isFullscreenWorkspace) {
            onToggleFullscreen(false)
        } else {
            currentTab = AppDestination.GATEWAY
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val hideChrome = currentTab == AppDestination.WORKSPACE && uiState.isFullscreenWorkspace

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = CyberBackground,
            topBar = {
                if (!hideChrome) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.isProxyRunning) MatrixGreen else AlertCrimson
                                        )
                                )
                                Text(
                                    text = if (uiState.isProxyRunning) {
                                        " :${uiState.activeProxyPort}"
                                    } else {
                                        " OFF"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.isProxyRunning) NeonCyan else TextSecondary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CyberSurface,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen && !hideChrome) {
                    NavigationBar(
                        containerColor = CyberSurface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelResId),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF001F26),
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${dest.routeId}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen && !hideChrome) {
                    NavigationRail(
                        containerColor = CyberSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_navigation_rail")
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelResId),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("rail_tab_${dest.routeId}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        AppDestination.GATEWAY -> GatewayDashboardScreen(
                            uiState = uiState,
                            onStartGateway = onStartGateway,
                            onStopGateway = onStopGateway,
                            onOpenWorkspaceTab = { currentTab = AppDestination.WORKSPACE },
                            onOpenApkGuideTab = { currentTab = AppDestination.APK_GUIDE },
                            onRunStatusCheck = {
                                onRunStatusCheck()
                                currentTab = AppDestination.CONSOLE
                            },
                            onRefreshCsrf = onRefreshCsrf,
                            onTriggerWatchdog = onTriggerWatchdog,
                            onSaveConfig = onSaveConfig
                        )

                        AppDestination.WORKSPACE -> WebWorkspaceScreen(
                            uiState = uiState,
                            onToggleFullscreen = onToggleFullscreen,
                            onToggleDesktopMode = onToggleDesktopMode
                        )

                        AppDestination.CONSOLE -> ConsoleLogsScreen(
                            uiState = uiState,
                            onExecuteCliCommand = onExecuteCliCommand,
                            onClearLogs = onClearLogs
                        )

                        AppDestination.APK_GUIDE -> ApkAndSetupGuideScreen()
                    }
                }
            }
        }
    }
}
