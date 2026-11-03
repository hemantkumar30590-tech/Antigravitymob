package com.example.gateway

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native Kotlin implementation of `src/proxy.js` and `bin/index.js` from
 * https://github.com/hemantkumar30590-tech/Antigravityweb-onmob
 *
 * Implements:
 * - Zero-config HTTP + WebSocket Reverse Proxy on 0.0.0.0:proxyPort (default 4400)
 * - Automatic CSRF Header Bridge (`x-agaqkz8vmwnqfj_9qx` & `x-codeium-csrf-token`)
 * - Self-Healing Watchdog (`checkBackendAlive` + `ensureBackendRunning` on port 1235)
 * - Mobile Keep-Alive & Socket Cleanup (`keep-alive`, `timeout=65`)
 * - Built-in interactive Antigravity Web IDE Portal when local port 1235 is on standby
 */
class AntigravityGatewayEngine(
    private val scope: CoroutineScope,
    private val onLog: (LogLevel, String, String) -> Unit,
    private val onStateChanged: () -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var watchdogJob: Job? = null
    private val connectionExecutor = Executors.newCachedThreadPool()

    val isRunning = AtomicBoolean(false)
    val isBackendUp = AtomicBoolean(false)
    var currentConfig = GatewayConfig()
        private set
    var activeBindPort: Int = 4400
        private set

    @Volatile
    var cachedCsrfToken: String = "907c52b4-6603-48d9-a40f-a2d710697713"
        private set

    val requestsProxied = AtomicInteger(0)
    val wsUpgradesProxied = AtomicInteger(0)
    val csrfInjectedCount = AtomicInteger(0)
    val watchdogChecks = AtomicInteger(0)
    val watchdogRecoveries = AtomicInteger(0)
    val startTimeEpochMs = AtomicLong(0L)

    private val csrfRegex = Regex(""""csrfToken"\s*:\s*"([^"]+)"""")

    /**
     * Equivalent to `getLanIPs()` in `src/proxy.js`
     */
    fun getLanIPs(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in Collections.list(interfaces)) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in Collections.list(nif.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val hostAddr = addr.hostAddress
                        if (!hostAddr.isNullOrBlank()) {
                            ips.add(hostAddr)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ips.distinct()
    }

    /**
     * Equivalent to `checkBackendAlive(port, host)` in `src/proxy.js`
     */
    suspend fun checkBackendAlive(
        port: Int = currentConfig.targetPort,
        host: String = currentConfig.targetHost
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1200)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Equivalent to `ensureBackendRunning(targetPort)` in `src/proxy.js`
     */
    suspend fun ensureBackendRunning(targetPort: Int = currentConfig.targetPort): Boolean =
        withContext(Dispatchers.IO) {
            val alive = checkBackendAlive(targetPort, currentConfig.targetHost)
            if (alive) {
                isBackendUp.set(true)
                return@withContext true
            }

            val homeDir = System.getenv("HOME") ?: "/data/data/com.termux/files/home"
            val prefixDir = System.getenv("PREFIX") ?: "/data/data/com.termux/files/usr"
            val bridgeBin = File("$prefixDir/bin/agy_ide_bridge")
            val agyBin = File("$homeDir/.local/bin/agy")
            val tokenFile = File("$homeDir/.antigem_bridge/csrf_token.txt")

            if (!bridgeBin.exists() || !agyBin.exists()) {
                isBackendUp.set(false)
                return@withContext false
            }

            onLog(
                LogLevel.WARN,
                "Watchdog",
                "⚡ [Watchdog] Antigravity backend is down! Auto-reviving backend on :$targetPort..."
            )

            var token = "aqkz8vmwnqfj"
            try {
                if (tokenFile.exists()) {
                    token = tokenFile.readText().trim().ifEmpty { token }
                }
            } catch (_: Exception) {
            }

            try {
                val pb = ProcessBuilder(
                    bridgeBin.absolutePath,
                    "-f",
                    "--token", token,
                    "-p", "1234",
                    "--hub-port", targetPort.toString(),
                    "--tz-offset", "19800",
                    "--bin", agyBin.absolutePath,
                    "--no-claude"
                )
                val env = pb.environment()
                env["NODE_OPTIONS"] = "--dns-result-order=ipv4first"
                env["GOMEMLIMIT"] = "1536MiB"
                pb.redirectErrorStream(true)
                pb.start()
            } catch (e: Exception) {
                onLog(LogLevel.ERROR, "Watchdog", "Failed to revive backend: ${e.message}")
            }

            for (i in 0 until 20) {
                delay(200)
                if (checkBackendAlive(targetPort, currentConfig.targetHost)) {
                    isBackendUp.set(true)
                    watchdogRecoveries.incrementAndGet()
                    onLog(LogLevel.SUCCESS, "Watchdog", "✅ [Watchdog] Backend restored successfully!")
                    return@withContext true
                }
            }
            isBackendUp.set(false)
            false
        }

    /**
     * Equivalent to `refreshCsrfToken()` in `src/proxy.js`
     */
    suspend fun refreshCsrfToken(): String = withContext(Dispatchers.IO) {
        val host = currentConfig.targetHost
        val port = currentConfig.targetPort
        try {
            val url = URL("http://$host:$port/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 2000
                requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code in 200..399) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val match = csrfRegex.find(body)
                if (match != null && match.groupValues.size > 1) {
                    val extracted = match.groupValues[1]
                    if (extracted.isNotBlank() && extracted != cachedCsrfToken) {
                        cachedCsrfToken = extracted
                        onLog(
                            LogLevel.SUCCESS,
                            "CSRF-Bridge",
                            "Refreshed live CSRF token from Hub :$port -> ${extracted.take(12)}..."
                        )
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
        cachedCsrfToken
    }

    /**
     * Starts the Gateway HTTP & WebSocket Reverse Proxy server (`startGateway(options)` in `src/proxy.js`)
     */
    fun startGateway(config: GatewayConfig = currentConfig) {
        stopGateway(silent = true)
        currentConfig = config
        cachedCsrfToken = config.csrfToken

        scope.launch(Dispatchers.IO) {
            var bindPort = config.proxyPort
            var boundSocket: ServerSocket? = null

            for (attempt in 0..3) {
                try {
                    val candidate = ServerSocket()
                    candidate.reuseAddress = true
                    candidate.bind(InetSocketAddress("0.0.0.0", bindPort), 128)
                    boundSocket = candidate
                    break
                } catch (e: Exception) {
                    onLog(
                        LogLevel.WARN,
                        "Gateway",
                        "Port $bindPort busy (${e.message}), trying ${bindPort + 1}..."
                    )
                    bindPort += 1
                }
            }

            if (boundSocket == null) {
                onLog(LogLevel.ERROR, "Gateway", "Failed to bind Gateway Proxy socket.")
                isRunning.set(false)
                onStateChanged()
                return@launch
            }

            serverSocket = boundSocket
            activeBindPort = bindPort
            isRunning.set(true)
            startTimeEpochMs.set(System.currentTimeMillis())

            val backendAlive = checkBackendAlive(config.targetPort, config.targetHost)
            isBackendUp.set(backendAlive)
            if (backendAlive) {
                refreshCsrfToken()
            } else if (config.autoReviveWatchdog) {
                ensureBackendRunning(config.targetPort)
            }

            val lanIps = getLanIPs()
            val localUrl = "http://localhost:$bindPort"
            onLog(LogLevel.INFO, "Gateway", "═".repeat(46))
            onLog(LogLevel.SUCCESS, "Gateway", "🚀 Google Antigravity Web Gateway is Active! 🚀")
            onLog(LogLevel.INFO, "Gateway", "📱 Mobile Browser URL  : $localUrl")
            lanIps.forEach { ip ->
                onLog(LogLevel.INFO, "Gateway", "💻 PC / Laptop (Wi-Fi) : http://$ip:$bindPort")
            }
            if (!isBackendUp.get()) {
                onLog(
                    LogLevel.WARN,
                    "Gateway",
                    "ℹ️ Hub :${config.targetPort} not detected locally — serving Built-in Antigravity Web IDE Portal on :$bindPort"
                )
            }
            onLog(LogLevel.INFO, "Gateway", "═".repeat(46))
            onStateChanged()

            // Start 10s Watchdog Loop (matching `setInterval(..., 10000)` in `src/proxy.js`)
            watchdogJob = scope.launch(Dispatchers.IO) {
                while (isActive && isRunning.get()) {
                    delay(10_000L)
                    watchdogChecks.incrementAndGet()
                    val alive = checkBackendAlive(currentConfig.targetPort, currentConfig.targetHost)
                    val previous = isBackendUp.getAndSet(alive)
                    if (!alive) {
                        if (previous) {
                            onLog(
                                LogLevel.WARN,
                                "Watchdog",
                                "Backend :${currentConfig.targetPort} connection lost. Running self-healing check..."
                            )
                        }
                        if (currentConfig.autoReviveWatchdog) {
                            ensureBackendRunning(currentConfig.targetPort)
                        }
                    } else {
                        if (!previous) {
                            onLog(
                                LogLevel.SUCCESS,
                                "Watchdog",
                                "Backend Hub :${currentConfig.targetPort} is now ONLINE! Switching proxy to live Hub."
                            )
                        }
                        refreshCsrfToken()
                    }
                    onStateChanged()
                }
            }

            // Accept incoming HTTP & WebSocket connections
            acceptJob = scope.launch(Dispatchers.IO) {
                while (isActive && isRunning.get()) {
                    try {
                        val client = boundSocket.accept()
                        client.keepAlive = true
                        client.soTimeout = 65_000
                        connectionExecutor.execute {
                            handleClientSocket(client)
                        }
                    } catch (_: Exception) {
                        if (!isRunning.get()) break
                    }
                }
            }
        }
    }

    fun stopGateway(silent: Boolean = false) {
        isRunning.set(false)
        acceptJob?.cancel()
        watchdogJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        if (!silent) {
            onLog(LogLevel.WARN, "Gateway", "Stopped Antigravity Web Gateway on port :$activeBindPort")
            onStateChanged()
        }
    }

    private fun handleClientSocket(clientSocket: Socket) {
        try {
            clientSocket.use { client ->
                val input = client.getInputStream()
                val output = client.getOutputStream()

                val headerLines = mutableListOf<String>()
                val lineBuffer = StringBuilder()
                var prevChar = -1
                while (true) {
                    val b = input.read()
                    if (b == -1) return
                    val c = b.toChar()
                    if (prevChar == '\r'.code && c == '\n') {
                        lineBuffer.setLength(lineBuffer.length - 1)
                        val line = lineBuffer.toString()
                        lineBuffer.setLength(0)
                        if (line.isEmpty()) break
                        headerLines.add(line)
                    } else {
                        lineBuffer.append(c)
                    }
                    prevChar = b
                }

                if (headerLines.isEmpty()) return
                val requestLine = headerLines.first()
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"
                val httpVersion = parts.getOrNull(2) ?: "HTTP/1.1"

                val headersMap = linkedMapOf<String, String>()
                for (i in 1 until headerLines.size) {
                    val hLine = headerLines[i]
                    val colonIdx = hLine.indexOf(':')
                    if (colonIdx > 0) {
                        val key = hLine.substring(0, colonIdx).trim()
                        val value = hLine.substring(colonIdx + 1).trim()
                        headersMap[key] = value
                    }
                }

                val isUpgrade = headersMap.entries.any {
                    it.key.equals("Upgrade", ignoreCase = true) &&
                        it.value.contains("websocket", ignoreCase = true)
                }

                // Read request body if Content-Length is present
                val contentLength = headersMap.entries
                    .firstOrNull { it.key.equals("Content-Length", ignoreCase = true) }
                    ?.value?.toIntOrNull() ?: 0
                val bodyBytes = if (contentLength > 0 && contentLength < 10 * 1024 * 1024) {
                    val buf = ByteArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = input.read(buf, readTotal, contentLength - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    buf
                } else {
                    ByteArray(0)
                }

                // Built-in Gateway diagnostic API endpoint
                if (path.startsWith("/__agy_gateway_status")) {
                    requestsProxied.incrementAndGet()
                    csrfInjectedCount.incrementAndGet()
                    serveGatewayJsonStatus(output)
                    onStateChanged()
                    return
                }

                val targetHost = currentConfig.targetHost
                val targetPort = currentConfig.targetPort
                val framedHeader = currentConfig.framedHeader
                val token = cachedCsrfToken.ifBlank {
                    headersMap["x-codeium-csrf-token"]
                        ?: headersMap[framedHeader]
                        ?: currentConfig.csrfToken
                }

                // Try connecting to Target Backend Hub (:1235)
                val targetSocket = try {
                    Socket().apply {
                        keepAlive = true
                        soTimeout = 65_000
                        connect(InetSocketAddress(targetHost, targetPort), 1200)
                    }
                } catch (_: Exception) {
                    null
                }

                if (targetSocket != null) {
                    isBackendUp.set(true)
                    targetSocket.use { backend ->
                        val backendOut = backend.getOutputStream()
                        val backendIn = backend.getInputStream()

                        val forwardedHeaders = linkedMapOf<String, String>()
                        for ((k, v) in headersMap) {
                            forwardedHeaders[k] = v
                        }
                        forwardedHeaders["Host"] = "localhost:$targetPort"
                        forwardedHeaders["x-forwarded-host"] = headersMap["Host"] ?: "localhost:$activeBindPort"
                        forwardedHeaders["x-forwarded-for"] = client.inetAddress?.hostAddress ?: "127.0.0.1"
                        if (token.isNotBlank()) {
                            forwardedHeaders[framedHeader] = token
                            forwardedHeaders["x-codeium-csrf-token"] = token
                            csrfInjectedCount.incrementAndGet()
                        }

                        val rawReqBuilder = StringBuilder()
                        rawReqBuilder.append("$method $path $httpVersion\r\n")
                        for ((k, v) in forwardedHeaders) {
                            rawReqBuilder.append("$k: $v\r\n")
                        }
                        rawReqBuilder.append("\r\n")
                        backendOut.write(rawReqBuilder.toString().toByteArray(Charsets.UTF_8))
                        if (bodyBytes.isNotEmpty()) {
                            backendOut.write(bodyBytes)
                        }
                        backendOut.flush()

                        if (isUpgrade) {
                            wsUpgradesProxied.incrementAndGet()
                            onLog(
                                LogLevel.PROXY,
                                "WS-Upgrade",
                                "⇄ WebSocket Upgrade bridged to $targetHost:$targetPort$path [CSRF injected]"
                            )
                            onStateChanged()

                            val t1 = Thread {
                                pipeStreams(input, backendOut)
                            }
                            val t2 = Thread {
                                pipeStreams(backendIn, output)
                            }
                            t1.start()
                            t2.start()
                            t1.join()
                            t2.join()
                        } else {
                            requestsProxied.incrementAndGet()
                            onLog(
                                LogLevel.PROXY,
                                "HTTP-Proxy",
                                "$method $path → $targetHost:$targetPort [CSRF: ${token.take(8)}…]"
                            )
                            onStateChanged()
                            pipeStreams(backendIn, output)
                        }
                    }
                } else {
                    // Backend (:1235) is not running locally yet -> Serve Built-in Antigravity Web IDE Workspace
                    isBackendUp.set(false)
                    requestsProxied.incrementAndGet()
                    csrfInjectedCount.incrementAndGet()
                    onLog(
                        LogLevel.PROXY,
                        "IDE-Portal",
                        "$method $path → Served Built-in Antigravity Mobile IDE Workspace [CSRF: ${token.take(8)}…]"
                    )
                    serveBuiltinAntigravityIdePortal(output, path, token)
                    onStateChanged()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        try {
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: Exception) {
        }
    }

    private fun serveGatewayJsonStatus(output: OutputStream) {
        val lanIpsJson = getLanIPs().joinToString(",") { "\"$it\"" }
        val json = """{
  "gateway": "ACTIVE",
  "proxyPort": $activeBindPort,
  "targetHost": "${currentConfig.targetHost}",
  "targetPort": ${currentConfig.targetPort},
  "backendHubAlive": ${isBackendUp.get()},
  "framedHeader": "${currentConfig.framedHeader}",
  "csrfToken": "$cachedCsrfToken",
  "requestsProxied": ${requestsProxied.get()},
  "wsUpgradesProxied": ${wsUpgradesProxied.get()},
  "csrfInjected": ${csrfInjectedCount.get()},
  "lanIps": [$lanIpsJson]
}"""
        val bytes = json.toByteArray(Charsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
            append("Connection: close\r\n")
            append("x-codeium-csrf-token: $cachedCsrfToken\r\n")
            append("${currentConfig.framedHeader}: $cachedCsrfToken\r\n")
            append("Content-Length: ${bytes.size}\r\n\r\n")
        }
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveBuiltinAntigravityIdePortal(
        output: OutputStream,
        path: String,
        token: String
    ) {
        val lanIps = getLanIPs()
        val lanUrl = lanIps.firstOrNull()?.let { "http://$it:$activeBindPort" } ?: "http://localhost:$activeBindPort"
        val html = BuiltinIdeHtmlProvider.buildInteractiveIdeHtml(
            proxyPort = activeBindPort,
            targetHost = currentConfig.targetHost,
            targetPort = currentConfig.targetPort,
            framedHeader = currentConfig.framedHeader,
            csrfToken = token,
            lanUrl = lanUrl,
            requestedPath = path
        )
        val bytes = html.toByteArray(Charsets.UTF_8)
        val responseHeaders = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
            append("Pragma: no-cache\r\n")
            append("Expires: 0\r\n")
            append("Connection: keep-alive\r\n")
            append("Keep-Alive: timeout=65\r\n")
            append("x-codeium-csrf-token: $token\r\n")
            append("${currentConfig.framedHeader}: $token\r\n")
            append("Content-Length: ${bytes.size}\r\n\r\n")
        }
        output.write(responseHeaders.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    companion object {
        fun nowTimeString(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }
    }
}
