package com.example.gateway

data class GatewayConfig(
    val targetHost: String = "127.0.0.1",
    val targetPort: Int = 1235,
    val proxyPort: Int = 4400,
    val framedHeader: String = "x-agaqkz8vmwnqfj_9qx",
    val csrfToken: String = "907c52b4-6603-48d9-a40f-a2d710697713",
    val autoReviveWatchdog: Boolean = true,
    val holdWakeLock: Boolean = true
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    PROXY
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class GatewayStats(
    val requestsProxied: Int = 0,
    val wsUpgradesProxied: Int = 0,
    val csrfTokensInjected: Int = 0,
    val watchdogChecks: Int = 0,
    val watchdogRecoveries: Int = 0,
    val uptimeSeconds: Long = 0L
)

enum class HubConnectionMode {
    LIVE_BACKEND_HUB,
    BUILTIN_IDE_PORTAL,
    STOPPED
}

data class GatewayUiState(
    val isProxyRunning: Boolean = false,
    val isBackendAlive: Boolean = false,
    val hubMode: HubConnectionMode = HubConnectionMode.STOPPED,
    val activeProxyPort: Int = 4400,
    val config: GatewayConfig = GatewayConfig(),
    val cachedCsrfToken: String = "907c52b4-6603-48d9-a40f-a2d710697713",
    val lanIps: List<String> = emptyList(),
    val isWakeLockHeld: Boolean = false,
    val stats: GatewayStats = GatewayStats(),
    val logs: List<LogEntry> = emptyList(),
    val workspaceUrl: String = "http://127.0.0.1:4400",
    val isFullscreenWorkspace: Boolean = false,
    val isDesktopViewport: Boolean = false
)

object RepoReferenceData {
    const val REPO_URL = "https://github.com/hemantkumar30590-tech/Antigravityweb-onmob"
    const val NPX_COMMAND = "npx github:hemantkumar30590-tech/Antigravityweb-onmob"
    const val NPX_GLOBAL_COMMAND = "npx antigravityweb-onmob"
    const val STATUS_COMMAND = "npx antigravityweb-onmob --status"
    const val STOP_COMMAND = "npx antigravityweb-onmob --stop"

    const val TERMUX_BRIDGE_CMD = """export NODE_OPTIONS="--dns-result-order=ipv4first"
export GOMEMLIMIT="1536MiB"
agy_ide_bridge -f --token aqkz8vmwnqfj -p 1234 --hub-port 1235 --tz-offset 19800 --bin ~/.local/bin/agy --no-claude"""

    const val GITHUB_ACTIONS_WORKFLOW_YAML = """name: Build Antigravity Web APK

on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build-apk:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload Compiled APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: AntigravityWeb-Mobile-APK
          path: app/build/outputs/apk/debug/app-debug.apk"""
}
