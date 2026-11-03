package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.gateway.GatewayUiState
import com.example.gateway.HubConnectionMode
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GatewayDashboardScreen(
    uiState: GatewayUiState,
    onStartGateway: () -> Unit,
    onStopGateway: () -> Unit,
    onOpenWorkspaceTab: () -> Unit,
    onOpenApkGuideTab: () -> Unit,
    onRunStatusCheck: () -> Unit,
    onRefreshCsrf: () -> Unit,
    onTriggerWatchdog: () -> Unit,
    onSaveConfig: (Int, String, Int, String, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfigEditor by remember { mutableStateOf(false) }

    var proxyPortInput by remember(uiState.config.proxyPort) {
        mutableStateOf(uiState.config.proxyPort.toString())
    }
    var targetHostInput by remember(uiState.config.targetHost) {
        mutableStateOf(uiState.config.targetHost)
    }
    var targetPortInput by remember(uiState.config.targetPort) {
        mutableStateOf(uiState.config.targetPort.toString())
    }
    var csrfTokenInput by remember(uiState.config.csrfToken) {
        mutableStateOf(uiState.config.csrfToken)
    }
    var autoReviveInput by remember(uiState.config.autoReviveWatchdog) {
        mutableStateOf(uiState.config.autoReviveWatchdog)
    }
    var holdWakeLockInput by remember(uiState.config.holdWakeLock) {
        mutableStateOf(uiState.config.holdWakeLock)
    }

    val mobileBrowserUrl = "http://localhost:${uiState.activeProxyPort}"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .testTag("gateway_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Gateway Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_gateway_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isProxyRunning) NeonCyan.copy(alpha = 0.5f) else BorderSubtle
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_gateway_banner_1793694525024),
                            contentDescription = stringResource(R.string.banner_content_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x99070B14),
                                            Color(0xEE070B14),
                                            CyberSurface
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    isActive = uiState.isProxyRunning,
                                    label = if (uiState.isProxyRunning) {
                                        "GATEWAY ACTIVE :${uiState.activeProxyPort}"
                                    } else {
                                        "GATEWAY STOPPED"
                                    }
                                )
                                Text(
                                    text = formatUptime(uiState.stats.uptimeSeconds),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Antigravityweb-onmob",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Zero-config Web Gateway, CSRF Header Bridge & Self-Healing Watchdog for Google Antigravity IDE on Android & LAN.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FeatureChip(
                                    icon = Icons.Default.Security,
                                    text = "CSRF Bridge (${uiState.config.framedHeader.take(10)}…)",
                                    tint = MatrixGreen
                                )
                                FeatureChip(
                                    icon = Icons.Default.Bolt,
                                    text = when (uiState.hubMode) {
                                        HubConnectionMode.LIVE_BACKEND_HUB -> "Hub :${uiState.config.targetPort} Connected"
                                        HubConnectionMode.BUILTIN_IDE_PORTAL -> "Built-in IDE Portal :${uiState.activeProxyPort}"
                                        HubConnectionMode.STOPPED -> "Proxy Offline"
                                    },
                                    tint = if (uiState.isBackendAlive) MatrixGreen else NeonCyan
                                )
                                if (uiState.isWakeLockHeld) {
                                    FeatureChip(
                                        icon = Icons.Default.Lock,
                                        text = "Wake-Lock Active",
                                        tint = WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Primary Action Controls (Start/Stop, Open Fullscreen Web IDE, APK Export Guide)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenWorkspaceTab,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_web_ide_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color(0xFF001F26)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Open Fullscreen IDE",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_open_workspace),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (uiState.isProxyRunning) {
                                OutlinedButton(
                                    onClick = onStopGateway,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("stop_gateway_button"),
                                    border = BorderStroke(1.dp, AlertCrimson),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Gateway",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop")
                                }
                            } else {
                                Button(
                                    onClick = onStartGateway,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("start_gateway_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MatrixGreen,
                                        contentColor = Color(0xFF00210B)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start Gateway",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    openExternalBrowser(context, mobileBrowserUrl)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_external_browser_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in External Browser",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Browser (:4400)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = onOpenApkGuideTab,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_apk_download_guide_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ElectricIndigo.copy(alpha = 0.25f),
                                    contentColor = Color(0xFFE2D6FF)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "APK Download Guide",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APK Download",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. Mobile & Cross-Device LAN Endpoints Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("endpoints_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Endpoints",
                                tint = NeonCyan
                            )
                            Text(
                                text = "Gateway Endpoints (Mobile & Wi-Fi LAN)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        EndpointRow(
                            label = "📱 Mobile Browser URL",
                            url = mobileBrowserUrl,
                            onCopy = { copyToClipboard(context, "Gateway URL", mobileBrowserUrl) },
                            onShare = { shareText(context, mobileBrowserUrl) },
                            testTagPrefix = "mobile_endpoint"
                        )

                        if (uiState.lanIps.isEmpty()) {
                            EndpointRow(
                                label = "💻 PC / Laptop (Same Wi-Fi)",
                                url = "http://<phone-ip>:${uiState.activeProxyPort}",
                                onCopy = {
                                    copyToClipboard(
                                        context,
                                        "LAN URL",
                                        "http://192.168.1.x:${uiState.activeProxyPort}"
                                    )
                                },
                                onShare = {
                                    shareText(context, "http://localhost:${uiState.activeProxyPort}")
                                },
                                testTagPrefix = "lan_fallback_endpoint"
                            )
                        } else {
                            uiState.lanIps.forEachIndexed { idx, ip ->
                                val lanUrl = "http://$ip:${uiState.activeProxyPort}"
                                EndpointRow(
                                    label = "💻 PC / Laptop Wi-Fi #${idx + 1}",
                                    url = lanUrl,
                                    onCopy = { copyToClipboard(context, "LAN URL", lanUrl) },
                                    onShare = { shareText(context, lanUrl) },
                                    testTagPrefix = "lan_endpoint_$idx"
                                )
                            }
                        }
                    }
                }
            }

            // 4. Live Reverse Proxy & Watchdog Telemetry Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            title = "HTTP Proxied",
                            value = uiState.stats.requestsProxied.toString(),
                            subtitle = "Keep-Alive 65s",
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            title = "WS Upgrades",
                            value = uiState.stats.wsUpgradesProxied.toString(),
                            subtitle = "Bidirectional Pipe",
                            accentColor = ElectricIndigo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryStatCard(
                            title = "CSRF Injected",
                            value = uiState.stats.csrfTokensInjected.toString(),
                            subtitle = "No-Black-Screen",
                            accentColor = MatrixGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryStatCard(
                            title = "Watchdog Scans",
                            value = "${uiState.stats.watchdogChecks} (${uiState.stats.watchdogRecoveries}↺)",
                            subtitle = "10s Self-Heal Loop",
                            accentColor = WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. CSRF Header Bridge & Watchdog Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = "CSRF & Watchdog Bridge",
                                    tint = MatrixGreen
                                )
                                Text(
                                    text = "CSRF Bridge & Watchdog Diagnostics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showConfigEditor = !showConfigEditor },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_config_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configure Ports & Token",
                                    tint = NeonCyan
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background( Color(0xFF070B14))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MonoKeyValueRow("Framed Header", uiState.config.framedHeader, NeonCyan)
                            MonoKeyValueRow("Secondary Header", "x-codeium-csrf-token", NeonCyan)
                            MonoKeyValueRow("Cached CSRF Token", uiState.cachedCsrfToken, MatrixGreen)
                            MonoKeyValueRow(
                                "Target Hub (:1235)",
                                "http://${uiState.config.targetHost}:${uiState.config.targetPort}",
                                WarningAmber
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRunStatusCheck,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("status_check_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Status Check",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("--status", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onRefreshCsrf,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("refresh_csrf_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync CSRF Token",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync CSRF", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onTriggerWatchdog,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("watchdog_revive_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Revive Hub",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Watchdog", maxLines = 1)
                            }
                        }

                        // Expandable Gateway Config Form
                        AnimatedVisibility(visible = showConfigEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = BorderSubtle)
                                Text(
                                    text = "Gateway CLI Options (--port / --target-port)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = proxyPortInput,
                                        onValueChange = { proxyPortInput = it },
                                        label = { Text("Gateway Port") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_proxy_port")
                                    )
                                    OutlinedTextField(
                                        value = targetPortInput,
                                        onValueChange = { targetPortInput = it },
                                        label = { Text("Hub Target Port") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_target_port")
                                    )
                                }

                                OutlinedTextField(
                                    value = targetHostInput,
                                    onValueChange = { targetHostInput = it },
                                    label = { Text("Backend Target Host (127.0.0.1 or LAN IP)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_target_host")
                                )

                                OutlinedTextField(
                                    value = csrfTokenInput,
                                    onValueChange = { csrfTokenInput = it },
                                    label = { Text("CSRF Token Override") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_csrf_token")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Self-Healing Watchdog (10s Auto-Revive)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = autoReviveInput,
                                        onCheckedChange = { autoReviveInput = it },
                                        modifier = Modifier.testTag("switch_auto_revive")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hold Android CPU Wake-Lock",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = holdWakeLockInput,
                                        onCheckedChange = { holdWakeLockInput = it },
                                        modifier = Modifier.testTag("switch_wake_lock")
                                    )
                                }

                                Button(
                                    onClick = {
                                        val pPort = proxyPortInput.toIntOrNull() ?: 4400
                                        val tPort = targetPortInput.toIntOrNull() ?: 1235
                                        onSaveConfig(
                                            pPort,
                                            targetHostInput,
                                            tPort,
                                            csrfTokenInput,
                                            autoReviveInput,
                                            holdWakeLockInput
                                        )
                                        showConfigEditor = false
                                        Toast.makeText(
                                            context,
                                            "Gateway restarted on port $pPort",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("apply_config_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Save Config",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apply & Restart Gateway")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(isActive: Boolean, label: String) {
    val dotColor = if (isActive) MatrixGreen else AlertCrimson
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(dotColor.copy(alpha = 0.16f))
            .border(1.dp, dotColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131D31))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun EndpointRow(
    label: String,
    url: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    testTagPrefix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = url,
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row {
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_copy_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $url",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share $url",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MonoKeyValueRow(key: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("UP %02d:%02d:%02d", hrs, mins, secs)
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Gateway URL"))
}

fun openExternalBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open external browser", Toast.LENGTH_SHORT).show()
    }
}
