package com.tefillin.shalom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AlertHistoryApp() }
    }
}

data class Alert(val area: String, val time: String)

@Composable
fun AlertHistoryApp() {
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("לחץ רענן") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun loadAlerts() {
        scope.launch {
            isLoading = true
            status = "טוען..."
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://www.oref.org.il/WarningMessages/History/AlertsHistory.json")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.setRequestProperty("Referer", "https://www.oref.org.il/")
                    conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()

                    val json = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    conn.disconnect()

                    if (json.isBlank() || json == "null" || json == "[]") {
                        return@withContext emptyList<Alert>()
                    }
                    val arr = JSONArray(json)
                    val list = mutableListOf<Alert>()
                    for (i in 0 until minOf(arr.length(), 20)) {
                        val obj = arr.getJSONObject(i)
                        val area = obj.optString("data", "לא ידוע")
                        val time = obj.optString("alertDate", "")
                        list.add(Alert(area, time))
                    }
                    list
                } catch (e: Exception) {
                    null
                }
            }
            when {
                result == null -> {
                    status = "❌ שגיאת חיבור"
                    alerts = emptyList()
                }
                result.isEmpty() -> {
                    status = "✅ אין אזעקות אחרונות"
                    alerts = emptyList()
                }
                else -> {
                    status = ""
                    alerts = result
                }
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚨 אזעקות אחרונות",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Button(
                onClick = { loadAlerts() },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B0000)),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (isLoading) "..." else "🔄 רענן",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            alerts.forEach { alert ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(Color(0xFF3A0000))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = alert.area,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (alert.time.isNotEmpty()) {
                            Text(
                                text = alert.time,
                                color = Color(0xFFAAAAAA),
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
