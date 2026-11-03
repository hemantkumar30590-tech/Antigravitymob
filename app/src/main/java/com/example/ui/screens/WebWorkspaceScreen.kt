package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gateway.GatewayUiState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebWorkspaceScreen(
    uiState: GatewayUiState,
    onToggleFullscreen: (Boolean) -> Unit,
    onToggleDesktopMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableIntStateOf(100) }

    val csrfHeaders = remember(uiState.cachedCsrfToken, uiState.config.framedHeader) {
        mapOf(
            "x-codeium-csrf-token" to uiState.cachedCsrfToken,
            uiState.config.framedHeader to uiState.cachedCsrfToken,
            "Cache-Control" to "no-cache"
        )
    }

    val targetUrl = uiState.workspaceUrl

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("web_workspace_screen")
    ) {
        // Top Browser & CSRF Bridge Bar
        Surface(
            color = CyberSurface,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "CSRF Bridge Active",
                        tint = MatrixGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = targetUrl,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isBackendAlive) {
                                "Bridged to Antigravity Hub :${uiState.config.targetPort}"
                            } else {
                                "Built-in Antigravity IDE Workspace (:${uiState.activeProxyPort})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onToggleDesktopMode()
                            webViewRef?.settings?.apply {
                                useWideViewPort = !uiState.isDesktopViewport
                                loadWithOverviewMode = !uiState.isDesktopViewport
                            }
                            webViewRef?.reload()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_viewport_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDesktopViewport) {
                                Icons.Default.Computer
                            } else {
                                Icons.Default.PhoneAndroid
                            },
                            contentDescription = "Toggle Desktop or Mobile Viewport",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            webViewRef?.loadUrl(targetUrl, csrfHeaders)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload IDE Workspace",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            openExternalBrowser(context, "http://localhost:${uiState.activeProxyPort}")
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_external_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Chrome/Brave",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onToggleFullscreen(!uiState.isFullscreenWorkspace)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("workspace_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isFullscreenWorkspace) {
                                Icons.Default.CloseFullscreen
                            } else {
                                Icons.Default.Fullscreen
                            },
                            contentDescription = "Toggle Fullscreen Mode",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = NeonCyan,
                trackColor = CyberSurface
            )
        }

        // Main Android WebView connected to our local Reverse Proxy Server (:4400)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("antigravity_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val nextUri = request?.url?.toString() ?: return false
                                view?.loadUrl(nextUri, csrfHeaders)
                                return true
                            }
                        }
                        loadUrl(targetUrl, csrfHeaders)
                        webViewRef = this
                    }
                },
                update = { view ->
                    if (view.url == null) {
                        view.loadUrl(targetUrl, csrfHeaders)
                    }
                }
            )
        }

        // Mobile Touch Coding Quick-Key Bar (Termux / Mobile IDE style)
        Surface(
            color = CyberSurfaceElevated,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickSymbols = listOf(
                    "TAB" to "  ",
                    "{" to "{",
                    "}" to "}",
                    "(" to "(",
                    ")" to ")",
                    "[" to "[",
                    "]" to "]",
                    ";" to ";",
                    "=>" to " => ",
                    "const" to "const ",
                    "async" to "async ",
                    "status" to "status"
                )
                quickSymbols.forEach { (label, insertValue) ->
                    Surface(
                        onClick = {
                            if (label == "status") {
                                webViewRef?.evaluateJavascript("if(window.pingGateway) window.pingGateway();", null)
                            } else {
                                val escaped = insertValue.replace("'", "\\'")
                                webViewRef?.evaluateJavascript(
                                    "if(window.InsertSymbol) window.InsertSymbol('$escaped');",
                                    null
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurface,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_key_$label")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (label == "status") MatrixGreen else NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
