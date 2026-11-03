package com.example.ui

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gateway.AntigravityGatewayEngine
import com.example.gateway.GatewayConfig
import com.example.gateway.GatewayStats
import com.example.gateway.GatewayUiState
import com.example.gateway.HubConnectionMode
import com.example.gateway.LogEntry
import com.example.gateway.LogLevel
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GatewayViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GatewayUiState())
    val uiState: StateFlow<GatewayUiState> = _uiState.asStateFlow()

    private val logIdCounter = AtomicLong(1L)
    private var wakeLock: PowerManager.WakeLock? = null

    private val engine = AntigravityGatewayEngine(
        scope = viewModelScope,
        onLog = { level, tag, message ->
            appendLog(level, tag, message)
        },
        onStateChanged = {
            syncEngineState()
        }
    )

    init {
        appendLog(
            LogLevel.INFO,
            "CLI",
            "Initializing Antigravityweb-onmob v1.0.0 (Android Native Gateway & Launcher)..."
        )
        startGateway()

        // 1-second ticker for live uptime & LAN IP refresh
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (engine.isRunning.get()) {
                    syncEngineState()
                }
            }
        }
    }

    fun startGateway(customConfig: GatewayConfig = _uiState.value.config) {
        if (customConfig.holdWakeLock) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }
        engine.startGateway(customConfig)
    }

    fun stopGateway() {
        engine.stopGateway(silent = false)
        releaseWakeLock()
        syncEngineState()
    }

    fun updateConfigAndRestart(
        proxyPort: Int,
        targetHost: String,
        targetPort: Int,
        csrfToken: String,
        autoReviveWatchdog: Boolean,
        holdWakeLock: Boolean
    ) {
        val sanitizedProxyPort = proxyPort.coerceIn(1024, 65535)
        val sanitizedTargetPort = targetPort.coerceIn(1, 65535)
        val cleanHost = targetHost.trim().ifEmpty { "127.0.0.1" }
        val cleanToken = csrfToken.trim().ifEmpty { "907c52b4-6603-48d9-a40f-a2d710697713" }

        val newConfig = _uiState.value.config.copy(
            proxyPort = sanitizedProxyPort,
            targetHost = cleanHost,
            targetPort = sanitizedTargetPort,
            csrfToken = cleanToken,
            autoReviveWatchdog = autoReviveWatchdog,
            holdWakeLock = holdWakeLock
        )
        _uiState.update { it.copy(config = newConfig) }
        appendLog(
            LogLevel.INFO,
            "Config",
            "Applying gateway config: listen=:$sanitizedProxyPort -> target=$cleanHost:$sanitizedTargetPort"
        )
        startGateway(newConfig)
    }

    fun runStatusCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            val cfg = engine.currentConfig
            val isTargetUp = engine.checkBackendAlive(cfg.targetPort, cfg.targetHost)
            val isProxyUp = engine.checkBackendAlive(engine.activeBindPort, "127.0.0.1")
            engine.isBackendUp.set(isTargetUp)

            appendLog(LogLevel.INFO, "Status", "--- Antigravity Web Status (--status) ---")
            appendLog(
                if (isTargetUp) LogLevel.SUCCESS else LogLevel.WARN,
                "Status",
                "Backend Hub (:${cfg.targetPort}): ${if (isTargetUp) "✅ ACTIVE (Live Bridge)" else "⚡ STANDBY (Built-in IDE Portal Active)"}"
            )
            appendLog(
                if (isProxyUp) LogLevel.SUCCESS else LogLevel.ERROR,
                "Status",
                "Gateway Proxy (:${engine.activeBindPort}): ${if (isProxyUp) "✅ ACTIVE" else "❌ STOPPED"}"
            )
            if (isProxyUp) {
                appendLog(
                    LogLevel.INFO,
                    "Status",
                    "📱 Browser URL: http://localhost:${engine.activeBindPort}"
                )
            }
            syncEngineState()
        }
    }

    fun triggerManualWatchdogRevive() {
        viewModelScope.launch {
            appendLog(
                LogLevel.INFO,
                "Watchdog",
                "Running manual ensureBackendRunning(:${engine.currentConfig.targetPort})..."
            )
            val revived = engine.ensureBackendRunning(engine.currentConfig.targetPort)
            if (!revived) {
                appendLog(
                    LogLevel.WARN,
                    "Watchdog",
                    "Local Termux binary agy_ide_bridge not running on :${engine.currentConfig.targetPort} — continuing with Built-in Antigravity Web IDE Portal on :${engine.activeBindPort}"
                )
            }
            syncEngineState()
        }
    }

    fun refreshCsrfTokenNow() {
        viewModelScope.launch {
            val token = engine.refreshCsrfToken()
            appendLog(
                LogLevel.INFO,
                "CSRF-Bridge",
                "Active CSRF Header (${engine.currentConfig.framedHeader}): $token"
            )
            syncEngineState()
        }
    }

    fun executeCliCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return
        appendLog(LogLevel.INFO, "CLI", "$ $cmd")

        when {
            cmd.contains("--status") || cmd == "status" -> runStatusCheck()
            cmd.contains("--stop") || cmd == "stop" -> stopGateway()
            cmd.contains("--start") || cmd == "start" -> startGateway()
            cmd.contains("--help") || cmd == "help" -> {
                appendLog(
                    LogLevel.INFO,
                    "CLI",
                    "Usage: npx antigravityweb-onmob [--status | --stop | --port=<port> | --target-port=<port> | --no-open]"
                )
            }
            cmd.contains("csrf") -> refreshCsrfTokenNow()
            cmd.contains("revive") || cmd.contains("watchdog") -> triggerManualWatchdogRevive()
            else -> {
                // Check for --port= or --target-port=
                var pPort = _uiState.value.config.proxyPort
                var tPort = _uiState.value.config.targetPort
                var changed = false
                cmd.split(" ").forEach { arg ->
                    if (arg.startsWith("--port=")) {
                        arg.substringAfter("--port=").toIntOrNull()?.let {
                            pPort = it
                            changed = true
                        }
                    }
                    if (arg.startsWith("--target-port=")) {
                        arg.substringAfter("--target-port=").toIntOrNull()?.let {
                            tPort = it
                            changed = true
                        }
                    }
                }
                if (changed) {
                    updateConfigAndRestart(
                        proxyPort = pPort,
                        targetHost = _uiState.value.config.targetHost,
                        targetPort = tPort,
                        csrfToken = _uiState.value.config.csrfToken,
                        autoReviveWatchdog = _uiState.value.config.autoReviveWatchdog,
                        holdWakeLock = _uiState.value.config.holdWakeLock
                    )
                } else {
                    runStatusCheck()
                }
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    fun setFullscreenWorkspace(fullscreen: Boolean) {
        _uiState.update { it.copy(isFullscreenWorkspace = fullscreen) }
    }

    fun toggleDesktopViewport() {
        _uiState.update { it.copy(isDesktopViewport = !it.isDesktopViewport) }
    }

    fun setWorkspaceUrl(url: String) {
        _uiState.update { it.copy(workspaceUrl = url) }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == true) return
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AntigravityWeb::GatewayWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
            appendLog(LogLevel.SUCCESS, "WakeLock", "🔒 CPU Wake-Lock acquired (Background Persistence Active)")
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                appendLog(LogLevel.INFO, "WakeLock", "🔓 CPU Wake-Lock released")
            }
        } catch (_: Exception) {
        }
    }

    private fun appendLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            id = logIdCounter.getAndIncrement(),
            timestamp = AntigravityGatewayEngine.nowTimeString(),
            level = level,
            tag = tag,
            message = message
        )
        _uiState.update { current ->
            val updated = (current.logs + entry).takeLast(160)
            current.copy(logs = updated)
        }
    }

    private fun syncEngineState() {
        val running = engine.isRunning.get()
        val backendAlive = engine.isBackendUp.get()
        val bindPort = engine.activeBindPort
        val uptimeSec = if (running && engine.startTimeEpochMs.get() > 0L) {
            ((System.currentTimeMillis() - engine.startTimeEpochMs.get()) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        val hubMode = when {
            !running -> HubConnectionMode.STOPPED
            backendAlive -> HubConnectionMode.LIVE_BACKEND_HUB
            else -> HubConnectionMode.BUILTIN_IDE_PORTAL
        }

        _uiState.update { prev ->
            prev.copy(
                isProxyRunning = running,
                isBackendAlive = backendAlive,
                hubMode = hubMode,
                activeProxyPort = bindPort,
                config = engine.currentConfig,
                cachedCsrfToken = engine.cachedCsrfToken,
                lanIps = engine.getLanIPs(),
                isWakeLockHeld = wakeLock?.isHeld == true,
                workspaceUrl = "http://127.0.0.1:$bindPort",
                stats = GatewayStats(
                    requestsProxied = engine.requestsProxied.get(),
                    wsUpgradesProxied = engine.wsUpgradesProxied.get(),
                    csrfTokensInjected = engine.csrfInjectedCount.get(),
                    watchdogChecks = engine.watchdogChecks.get(),
                    watchdogRecoveries = engine.watchdogRecoveries.get(),
                    uptimeSeconds = uptimeSec
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.stopGateway(silent = true)
        releaseWakeLock()
    }
}
