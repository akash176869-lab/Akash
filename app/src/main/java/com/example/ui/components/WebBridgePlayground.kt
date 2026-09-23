package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bridge.AndroidBridge

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebBridgePlayground(
    androidBridge: AndroidBridge,
    modifier: Modifier = Modifier
) {
    val sampleHtml = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
              background-color: #0F1221;
              color: #E2E8F0;
              padding: 16px;
            }
            .card {
              background: #1A1F36;
              border: 1px solid #2E3654;
              border-radius: 14px;
              padding: 16px;
              margin-bottom: 16px;
            }
            .badge {
              display: inline-block;
              background: #10B981;
              color: #064E3B;
              font-weight: 700;
              font-size: 11px;
              padding: 4px 8px;
              border-radius: 6px;
              margin-bottom: 8px;
            }
            .badge.missing {
              background: #F59E0B;
              color: #78350F;
            }
            h2 { font-size: 18px; margin-bottom: 8px; color: #FFFFFF; }
            p { font-size: 13px; color: #94A3B8; line-height: 1.4; margin-bottom: 12px; }
            .btn-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 10px;
            }
            button {
              background: #7C3AED;
              color: white;
              border: none;
              padding: 12px 10px;
              border-radius: 10px;
              font-size: 13px;
              font-weight: 600;
              cursor: pointer;
              transition: background 0.2s;
              text-align: center;
            }
            button:active { background: #6D28D9; }
            button.whatsapp { background: #25D366; color: #075E54; }
            button.phone { background: #0EA5E9; }
            button.danger { background: #EF4444; }
            #console-box {
              background: #0B0D18;
              border: 1px solid #1F2540;
              border-radius: 10px;
              padding: 12px;
              font-family: monospace;
              font-size: 12px;
              color: #10B981;
              min-height: 120px;
              max-height: 220px;
              overflow-y: auto;
              white-space: pre-wrap;
              word-break: break-all;
            }
          </style>
        </head>
        <body>
          <div class="card">
            <span id="bridge-status" class="badge">Checking Bridge...</span>
            <h2>Android JavaScript Bridge</h2>
            <p>This page tests the <code>window.AndroidBridge</code> interface required by Arushi for Android app and action control.</p>
            
            <div class="btn-grid">
              <button class="whatsapp" onclick="testWhatsApp()">Open WhatsApp</button>
              <button onclick="testOpenApp('YouTube')">Open YouTube</button>
              <button onclick="testOpenApp('Settings')">Open Settings</button>
              <button class="phone" onclick="testCall('9876543210')">Call 9876543210</button>
              <button class="phone" onclick="testCallContact('Mom')">Call Contact: Mom</button>
              <button class="phone" onclick="testCallContact('Rahul')">Call Contact: Rahul</button>
              <button onclick="testUrl('https://google.com')">Open Google.com</button>
              <button class="danger" onclick="clearLogs()">Clear Console</button>
            </div>
          </div>

          <div class="card">
            <h2>Bridge Output Log</h2>
            <div id="console-box">Ready. Tap any action button above to invoke AndroidBridge.</div>
          </div>

          <script>
            function log(msg) {
              const box = document.getElementById('console-box');
              const time = new Date().toLocaleTimeString();
              box.textContent = '[' + time + '] ' + msg + '\n' + box.textContent;
            }

            function checkBridge() {
              const status = document.getElementById('bridge-status');
              if (window.AndroidBridge && typeof window.AndroidBridge.isNativeBridgeAvailable === 'function' && window.AndroidBridge.isNativeBridgeAvailable()) {
                status.className = 'badge';
                status.textContent = '🟢 window.AndroidBridge Active';
                log('Native AndroidBridge detected and verified.');
              } else {
                status.className = 'badge missing';
                status.textContent = '🟡 Web Fallback Mode';
                log('Native AndroidBridge not present. Web fallbacks will be used.');
              }
            }

            function testWhatsApp() {
              if (window.AndroidBridge) {
                log('Invoking AndroidBridge.openWhatsApp()...');
                const res = window.AndroidBridge.openWhatsApp();
                log('Result: ' + res);
              } else {
                log('Fallback: Redirecting to https://wa.me/...');
                window.open('https://wa.me/', '_blank');
              }
            }

            function testOpenApp(appName) {
              if (window.AndroidBridge) {
                log('Invoking AndroidBridge.openApp("' + appName + '")...');
                const res = window.AndroidBridge.openApp(appName);
                log('Result: ' + res);
              } else {
                log('Fallback: Unable to open native apps in plain web.');
              }
            }

            function testCall(number) {
              if (window.AndroidBridge) {
                log('Invoking AndroidBridge.makeCall("' + number + '")...');
                const res = window.AndroidBridge.makeCall(number);
                log('Result: ' + res);
              } else {
                log('Fallback: tel:' + number);
                window.location.href = 'tel:' + number;
              }
            }

            function testCallContact(name) {
              if (window.AndroidBridge) {
                log('Invoking AndroidBridge.callContact("' + name + '")...');
                const res = window.AndroidBridge.callContact(name);
                log('Result: ' + res);
              } else {
                log('Fallback: Cannot read device contacts in plain browser.');
              }
            }

            function testUrl(url) {
              if (window.AndroidBridge) {
                log('Invoking AndroidBridge.openUrl("' + url + '")...');
                const res = window.AndroidBridge.openUrl(url);
                log('Result: ' + res);
              } else {
                window.open(url, '_blank');
              }
            }

            function clearLogs() {
              document.getElementById('console-box').textContent = 'Console cleared.';
            }

            // Run check on load
            checkBridge();
          </script>
        </body>
        </html>
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_bridge_playground")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Bridge Code",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Android App Action Bridge",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real window.AndroidBridge JavaScript interface",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(androidBridge, "AndroidBridge")
                    loadDataWithBaseURL("https://localhost", sampleHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
