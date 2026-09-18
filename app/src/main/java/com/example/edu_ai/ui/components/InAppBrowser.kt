package com.example.edu_ai.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppBrowser(
    url: String,
    onClose: () -> Unit,
    onNavigate: ((String) -> Unit)? = null
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var address by remember { mutableStateOf(url) }
    var history by remember { mutableStateOf(listOf<String>()) }
    var showHistory by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                title = {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { Text("Search or enter a URL") }
                    )
                },
                actions = {
                    IconButton(onClick = {
                        webView?.loadDataWithBaseURL(
                            null,
                            "<html><body style='background:#101426;color:white;font-family:sans-serif;padding:32px'>" +
                                "<h1 style='color:#8b7cff'>TRACE</h1><h2>Your learning trail, connected.</h2>" +
                                "<p>Use the address bar to explore the web while keeping your learning context close.</p>" +
                                "</body></html>",
                            "text/html",
                            "UTF-8",
                            null
                        )
                        address = ""
                    }) { Icon(Icons.Default.Home, contentDescription = "Trace home") }
                    IconButton(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Text("H", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                )
                if (showHistory) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(history) { item ->
                            TextButton(onClick = { address = item; webView?.loadUrl(item); showHistory = false }) {
                                Text(item, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            address = pageUrl.takeUnless { it == "about:blank" } ?: ""
                            if (pageUrl != "about:blank" && pageUrl !in history) {
                                history = (history + pageUrl).takeLast(20)
                            }
                            onNavigate?.invoke(pageUrl)
                        }
                    }
                    settings.javaScriptEnabled = true
                    if (url.isNotBlank()) {
                        loadUrl(url)
                    } else {
                        loadDataWithBaseURL(
                            null,
                            "<html><body style='background:#101426;color:white;font-family:sans-serif;padding:32px'>" +
                                "<h1 style='color:#8b7cff'>TRACE</h1><h2>Your learning trail, connected.</h2>" +
                                "<p>Search the web, revisit history, and keep your discoveries beside your course work.</p>" +
                                "</body></html>",
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
