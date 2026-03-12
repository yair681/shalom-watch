package com.tefillin.shalom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AlertApp() }
    }
}

@Composable
fun AlertApp() {
    var status by remember { mutableStateOf("לחץ לבדיקה") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val bgColor = Color(0xFF1A0000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚨 פיקוד העורף",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = status,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        status = "בודק..."
                        status = withContext(Dispatchers.IO) {
                            try {
                                val json = URL("https://www.oref.org.il/WarningMessages/alert/alerts.json")
                                    .readText(Charsets.UTF_8)
                                if (json.isBlank() || json == "null") {
                                    "✅ אין אזעקות"
                                } else {
                                    val obj = JSONObject(json)
                                    val data = obj.optJSONArray("data")
                                    if (data == null || data.length() == 0) {
                                        "✅ אין אזעקות"
                                    } else {
                                        "🚨 ${data.getString(0)}"
                                    }
                                }
                            } catch (e: Exception) {
                                "❌ שגיאת חיבור"
                            }
                        }
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF8B0000)
                )
            ) {
                Text(
                    text = if (isLoading) "..." else "בדוק",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}
