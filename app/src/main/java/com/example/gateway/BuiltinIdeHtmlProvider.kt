package com.example.gateway

object BuiltinIdeHtmlProvider {
    fun buildInteractiveIdeHtml(
        proxyPort: Int,
        targetHost: String,
        targetPort: Int,
        framedHeader: String,
        csrfToken: String,
        lanUrl: String,
        requestedPath: String
    ): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<title>Antigravity Web IDE — Mobile Gateway :$proxyPort</title>
<style>
  :root {
    --bg: #070B14;
    --panel: #0F1626;
    --panel-alt: #162038;
    --border: #253554;
    --cyan: #00E5FF;
    --indigo: #7C4DFF;
    --green: #00E676;
    --amber: #FFB300;
    --text: #F0F4FF;
    --muted: #94A3B8;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: var(--bg);
    color: var(--text);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
  }
  .topbar {
    background: var(--panel);
    border-bottom: 1px solid var(--border);
    padding: 8px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 700;
    font-size: 14px;
    color: var(--cyan);
  }
  .badge {
    background: rgba(0, 230, 118, 0.14);
    color: var(--green);
    border: 1px solid rgba(0, 230, 118, 0.4);
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 999px;
    font-family: monospace;
  }
  .tabs {
    display: flex;
    background: #0A101E;
    border-bottom: 1px solid var(--border);
    overflow-x: auto;
  }
  .tab {
    padding: 9px 14px;
    font-size: 12px;
    font-family: monospace;
    color: var(--muted);
    border-right: 1px solid var(--border);
    cursor: pointer;
    white-space: nowrap;
    user-select: none;
  }
  .tab.active {
    background: var(--panel);
    color: var(--cyan);
    border-bottom: 2px solid var(--cyan);
    font-weight: 600;
  }
  .workspace {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .editor-wrap {
    flex: 1;
    display: flex;
    overflow: hidden;
    position: relative;
  }
  .line-nums {
    background: #090E1A;
    color: #475569;
    padding: 12px 8px;
    text-align: right;
    font-family: "JetBrains Mono", monospace;
    font-size: 12px;
    line-height: 1.55;
    user-select: none;
    border-right: 1px solid var(--border);
    min-width: 38px;
  }
  textarea#code-editor {
    flex: 1;
    background: var(--bg);
    color: #E2E8F0;
    border: none;
    outline: none;
    padding: 12px;
    font-family: "JetBrains Mono", monospace;
    font-size: 12.5px;
    line-height: 1.55;
    resize: none;
    white-space: pre;
    overflow: auto;
  }
  .terminal {
    height: 195px;
    background: #090D16;
    border-top: 1px solid var(--border);
    display: flex;
    flex-direction: column;
  }
  .term-header {
    padding: 6px 12px;
    background: var(--panel);
    border-bottom: 1px solid var(--border);
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 11px;
    font-family: monospace;
    color: var(--muted);
  }
  .term-actions button {
    background: var(--panel-alt);
    color: var(--cyan);
    border: 1px solid var(--border);
    border-radius: 4px;
    padding: 3px 8px;
    font-size: 11px;
    cursor: pointer;
    margin-left: 4px;
  }
  .term-body {
    flex: 1;
    padding: 8px 12px;
    font-family: monospace;
    font-size: 11.5px;
    line-height: 1.45;
    overflow-y: auto;
    color: #CBD5E1;
  }
  .term-input-row {
    display: flex;
    align-items: center;
    border-top: 1px solid #1E293B;
    padding: 4px 10px;
    background: #05080F;
  }
  .term-prompt {
    color: var(--green);
    font-family: monospace;
    font-size: 12px;
    margin-right: 6px;
  }
  .term-input-row input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    color: var(--text);
    font-family: monospace;
    font-size: 12px;
  }
</style>
</head>
<body>
  <div class="topbar">
    <div class="brand">⚡ Antigravity Web IDE</div>
    <div class="badge">CSRF BRIDGE :$proxyPort → :$targetPort</div>
  </div>
  <div class="tabs">
    <div class="tab active" onclick="openFile('proxy.js', this)">src/proxy.js</div>
    <div class="tab" onclick="openFile('index.js', this)">bin/index.js</div>
    <div class="tab" onclick="openFile('package.json', this)">package.json</div>
    <div class="tab" onclick="openFile('headers.json', this)">csrf_headers.json</div>
  </div>
  <div class="workspace">
    <div class="editor-wrap">
      <div class="line-nums" id="line-nums">1</div>
      <textarea id="code-editor" spellcheck="false" oninput="updateLines()"></textarea>
    </div>
    <div class="terminal">
      <div class="term-header">
        <span>TERMINAL · GATEWAY RPC INSPECTOR ($lanUrl)</span>
        <div class="term-actions">
          <button onclick="pingGateway()">Ping Proxy</button>
          <button onclick="checkStatus()">--status</button>
          <button onclick="clearTerm()">Clear</button>
        </div>
      </div>
      <div class="term-body" id="term-output">
        <div style="color:#00E5FF">═══ Google Antigravity Web Gateway Active on :$proxyPort ═══</div>
        <div>• Framed Header : <span style="color:#00E676">$framedHeader</span></div>
        <div>• Injected CSRF : <span style="color:#FFB300">$csrfToken</span></div>
        <div>• Target Hub    : http://$targetHost:$targetPort (Auto-Watchdog Active)</div>
      </div>
      <div class="term-input-row">
        <span class="term-prompt">agy@android:~$</span>
        <input id="cmd-input" type="text" placeholder="Type command (e.g. status, csrf, help) & press Enter" onkeydown="if(event.key==='Enter')runCmd()" />
      </div>
    </div>
  </div>
<script>
const FILES = {
  'proxy.js': `// Antigravityweb-onmob — src/proxy.js (Active on Android Port $proxyPort)
const TARGET_HOST = '$targetHost';
const TARGET_PORT = $targetPort;
const LISTEN_PORT = $proxyPort;
const FRAMED_HEADER = '$framedHeader';

// Injected CSRF Token Preventing Mobile Black-Screen:
let cachedCsrfToken = '$csrfToken';

// Self-Healing Watchdog + Keep-Alive Proxy Server
// Requests to http://localhost:$proxyPort automatically inject:
//   x-codeium-csrf-token: $csrfToken
//   $framedHeader: $csrfToken`,
  'index.js': `#!/usr/bin/env node
// bin/index.js — CLI Launcher for Antigravity Web Gateway
// Usage: npx github:hemantkumar30590-tech/Antigravityweb-onmob
import { startGateway, checkBackendAlive, ensureBackendRunning } from '../src/proxy.js';

const proxyPort = $proxyPort;
const targetPort = $targetPort;

await ensureBackendRunning(targetPort);
startGateway({ targetPort, proxyPort, autoOpen: true });`,
  'package.json': `{
  "name": "antigravityweb-onmob",
  "version": "1.0.0",
  "description": "Zero-config web gateway & launcher for Google Antigravity on mobile browsers and LAN",
  "main": "src/proxy.js",
  "bin": {
    "antigravityweb-onmob": "bin/index.js",
    "antigravity-web": "bin/index.js",
    "agy-web": "bin/index.js"
  }
}`,
  'headers.json': `{
  "x-codeium-csrf-token": "$csrfToken",
  "$framedHeader": "$csrfToken",
  "Host": "localhost:$targetPort",
  "Connection": "keep-alive",
  "Keep-Alive": "timeout=65",
  "Cache-Control": "no-cache, no-store, must-revalidate"
}`
};

function openFile(name, el) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  if (el) el.classList.add('active');
  document.getElementById('code-editor').value = FILES[name] || '';
  updateLines();
}

function updateLines() {
  const text = document.getElementById('code-editor').value;
  const count = Math.max(1, text.split('\\n').length);
  let nums = '';
  for (let i = 1; i <= count; i++) nums += i + '<br/>';
  document.getElementById('line-nums').innerHTML = nums;
}

function logTerm(html) {
  const out = document.getElementById('term-output');
  const div = document.createElement('div');
  div.innerHTML = html;
  out.appendChild(div);
  out.scrollTop = out.scrollHeight;
}

async function pingGateway() {
  try {
    const res = await fetch('/__agy_gateway_status');
    const data = await res.json();
    logTerm('<span style="color:#00E676">✔ Live Gateway RPC Response:</span> proxy=:' + data.proxyPort + ' | csrf=' + data.csrfToken.substring(0, 12) + '... | requests=' + data.requestsProxied);
  } catch (e) {
    logTerm('<span style="color:#FF5252">✖ Gateway RPC Error: ' + e.message + '</span>');
  }
}

function checkStatus() {
  pingGateway();
}

function clearTerm() {
  document.getElementById('term-output').innerHTML = '';
}

function runCmd() {
  const input = document.getElementById('cmd-input');
  const cmd = input.value.trim();
  if (!cmd) return;
  input.value = '';
  logTerm('<span style="color:#00E5FF">$ ' + cmd + '</span>');
  if (cmd.includes('status') || cmd === 'ping') {
    pingGateway();
  } else if (cmd.includes('csrf')) {
    logTerm('Active CSRF Bridge Header: $framedHeader = $csrfToken');
  } else if (cmd.includes('help')) {
    logTerm('Commands: status | ping | csrf | clear');
  } else if (cmd === 'clear') {
    clearTerm();
  } else {
    logTerm('Executed via Antigravity Mobile Gateway (:4400 -> :1235)');
  }
}

window.InsertSymbol = function(sym) {
  const ed = document.getElementById('code-editor');
  const start = ed.selectionStart || 0;
  const end = ed.selectionEnd || 0;
  const val = ed.value;
  ed.value = val.substring(0, start) + sym + val.substring(end);
  ed.selectionStart = ed.selectionEnd = start + sym.length;
  ed.focus();
  updateLines();
};

openFile('proxy.js', document.querySelector('.tab'));
</script>
</body>
</html>"""
    }
}
