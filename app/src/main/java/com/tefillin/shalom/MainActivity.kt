package com.tefillin.shalom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

enum class Screen { MENU, PRAYER }

// ======================================
// הכנס כאן את טקסט שחרית
// ======================================
val SHACHARIT_TEXT = """
הכנס כאן את טקסט שחרית
""".trimIndent()

// ======================================
// הכנס כאן את טקסט מנחה
// ======================================
val MINCHA_TEXT = """
הכנס כאן את טקסט מנחה
""".trimIndent()

// ======================================
// הכנס כאן את טקסט ערבית
// ======================================
val ARVIT_TEXT = """
הכנס כאן את טקסט ערבית
""".trimIndent()

// ======================================
// הכנס כאן את טקסט ברכת המזון
// ======================================
val BIRKAT_TEXT = """
הכנס כאן את טקסט ברכת המזון
""".trimIndent()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SiddurApp() }
    }
}

@Composable
fun SiddurApp() {
    var screen by remember { mutableStateOf(Screen.MENU) }
    var currentPrayer by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("") }

    when (screen) {
        Screen.MENU -> MenuScreen(
            onSelect = { title, text ->
                currentTitle = title
                currentPrayer = text
                screen = Screen.PRAYER
            }
        )
        Screen.PRAYER -> PrayerScreen(
            title = currentTitle,
            text = currentPrayer,
            onBack = { screen = Screen.MENU }
        )
    }
}

@Composable
fun MenuScreen(onSelect: (String, String) -> Unit) {
    val bgColor = Color(0xFF0A1A4A)
    val prayers = listOf(
        Triple("🌅 שחרית", SHACHARIT_TEXT, Color(0xFFFFD700)),
        Triple("☀️ מנחה", MINCHA_TEXT, Color(0xFFFFA500)),
        Triple("🌙 ערבית", ARVIT_TEXT, Color(0xFF60A5FA)),
        Triple("🍞 ברכת המזון", BIRKAT_TEXT, Color(0xFF4ADE80)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✡ סידור",
                color = Color(0xFFFFD700),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            prayers.forEach { (title, text, color) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF1E3A8A))
                        .clickable { onSelect(title, text) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrayerScreen(title: String, text: String, onBack: () -> Unit) {
    var fontSize by remember { mutableStateOf(14) }
    val bgColor = Color(0xFF0A1A4A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp)
        ) {
            // כותרת + חזרה
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀",
                    color = Color(0xFF60A5FA),
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(
                    text = title,
                    color = Color(0xFFFFD700),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF3B82F6))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6).sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // כפתורי גופן
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .background(bgColor.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (fontSize > 10) fontSize-- },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A))
            ) {
                Text(text = "א-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "$fontSize", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { if (fontSize < 24) fontSize++ },
                modifier = Modifier.size(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A))
            ) {
                Text(text = "א+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
