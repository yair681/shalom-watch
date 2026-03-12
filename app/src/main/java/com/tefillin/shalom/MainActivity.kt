package com.spacedodge.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay
import kotlin.random.Random

// ─── קבועים ───────────────────────────────────────
const val SCREEN_SIZE   = 200f   // גודל אזור המשחק (dp מדומה)
const val SHIP_SIZE     = 10f
const val OBSTACLE_W    = 18f
const val OBSTACLE_H    = 12f
const val STAR_COUNT    = 30
const val GAME_TICK_MS  = 30L    // ~33fps

enum class GameState { MENU, PLAYING, DEAD }

data class Obstacle(val x: Float, val y: Float)
data class Star(val x: Float, val y: Float, val size: Float, val speed: Float)
data class Explosion(val x: Float, val y: Float, var frame: Int = 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpaceDodgeGame() }
    }
}

@Composable
fun SpaceDodgeGame() {
    var gameState   by remember { mutableStateOf(GameState.MENU) }
    var shipX       by remember { mutableStateOf(100f) }
    var shipY       by remember { mutableStateOf(160f) }
    var obstacles   by remember { mutableStateOf(listOf<Obstacle>()) }
    var score       by remember { mutableStateOf(0) }
    var highScore   by remember { mutableStateOf(0) }
    var stars       by remember { mutableStateOf(generateStars()) }
    var explosion   by remember { mutableStateOf<Explosion?>(null) }
    var speed       by remember { mutableStateOf(3f) }
    var tick        by remember { mutableStateOf(0) }

    // ─── Game loop ───────────────────────────────
    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect

        // אתחול
        shipX = 100f; shipY = 160f
        obstacles = listOf()
        score = 0; speed = 3f; tick = 0

        while (gameState == GameState.PLAYING) {
            delay(GAME_TICK_MS)
            tick++

            // הזז כוכבים
            stars = stars.map { s ->
                val ny = s.y + s.speed
                if (ny > SCREEN_SIZE) s.copy(y = 0f, x = Random.nextFloat() * SCREEN_SIZE)
                else s.copy(y = ny)
            }

            // הזז מכשולים
            val moved = obstacles.map { it.copy(y = it.y + speed) }
                .filter { it.y < SCREEN_SIZE + OBSTACLE_H }
            obstacles = moved

            // הוסף מכשול
            val spawnRate = maxOf(20, 60 - score / 5)
            if (tick % spawnRate == 0) {
                val nx = Random.nextFloat() * (SCREEN_SIZE - OBSTACLE_W)
                obstacles = obstacles + Obstacle(nx, -OBSTACLE_H)
            }

            // עלה מהירות
            speed = 3f + score * 0.05f

            // עלה ניקוד
            if (tick % 20 == 0) score++

            // בדוק התנגשות
            val hit = obstacles.any { obs ->
                shipX + SHIP_SIZE > obs.x &&
                shipX - SHIP_SIZE < obs.x + OBSTACLE_W &&
                shipY + SHIP_SIZE > obs.y &&
                shipY - SHIP_SIZE < obs.y + OBSTACLE_H
            }

            if (hit) {
                explosion = Explosion(shipX, shipY)
                if (score > highScore) highScore = score
                gameState = GameState.DEAD
            }
        }
    }

    // ─── אנימציית פיצוץ ─────────────────────────
    LaunchedEffect(gameState) {
        if (gameState != GameState.DEAD) return@LaunchedEffect
        repeat(12) {
            delay(50)
            explosion = explosion?.copy(frame = (explosion?.frame ?: 0) + 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000814)),
        contentAlignment = Alignment.Center
    ) {
        when (gameState) {
            GameState.MENU -> MenuScreen(highScore) { gameState = GameState.PLAYING }
            GameState.DEAD -> DeadScreen(score, highScore) { gameState = GameState.PLAYING }
            GameState.PLAYING -> {
                GameCanvas(
                    shipX = shipX,
                    shipY = shipY,
                    obstacles = obstacles,
                    stars = stars,
                    explosion = explosion,
                    score = score,
                    onTapLeft  = { shipX = (shipX - 20f).coerceIn(SHIP_SIZE, SCREEN_SIZE - SHIP_SIZE) },
                    onTapRight = { shipX = (shipX + 20f).coerceIn(SHIP_SIZE, SCREEN_SIZE - SHIP_SIZE) }
                )
            }
        }
    }
}

// ─── מסך משחק ─────────────────────────────────────
@Composable
fun GameCanvas(
    shipX: Float, shipY: Float,
    obstacles: List<Obstacle>,
    stars: List<Star>,
    explosion: Explosion?,
    score: Int,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (offset.x < size.width / 2) onTapLeft() else onTapRight()
                    }
                }
        ) {
            val scaleX = size.width  / SCREEN_SIZE
            val scaleY = size.height / SCREEN_SIZE

            fun sx(v: Float) = v * scaleX
            fun sy(v: Float) = v * scaleY

            // כוכבים
            stars.forEach { s ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f + s.size * 0.2f),
                    radius = s.size * scaleX,
                    center = Offset(sx(s.x), sy(s.y))
                )
            }

            // מכשולים — אסטרואידים
            obstacles.forEach { obs ->
                drawAsteroid(sx(obs.x), sy(obs.y), sx(OBSTACLE_W), sy(OBSTACLE_H))
            }

            // ספינה
            if (explosion == null || explosion.frame < 3) {
                drawShip(sx(shipX), sy(shipY), sx(SHIP_SIZE))
            }

            // פיצוץ
            explosion?.let { exp ->
                val r = exp.frame * sx(4f)
                val alpha = 1f - exp.frame / 12f
                drawCircle(Color(0xFFFF6B00).copy(alpha = alpha), r * 1.2f, Offset(sx(exp.x), sy(exp.y)))
                drawCircle(Color(0xFFFFD700).copy(alpha = alpha), r * 0.7f, Offset(sx(exp.x), sy(exp.y)))
                drawCircle(Color.White.copy(alpha = alpha * 0.5f), r * 0.3f, Offset(sx(exp.x), sy(exp.y)))
            }
        }

        // ניקוד
        Text(
            text = "⭐ $score",
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
        )

        // רמזי כיוון
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("◀", color = Color(0xFF60A5FA).copy(alpha = 0.5f), fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp))
            Text("▶", color = Color(0xFF60A5FA).copy(alpha = 0.5f), fontSize = 10.sp,
                modifier = Modifier.padding(end = 8.dp))
        }
    }
}

fun DrawScope.drawShip(cx: Float, cy: Float, s: Float) {
    // גוף הספינה
    drawRect(
        color = Color(0xFF60A5FA),
        topLeft = Offset(cx - s * 0.4f, cy - s * 0.8f),
        size = Size(s * 0.8f, s * 1.6f)
    )
    // כנפיים
    drawRect(
        color = Color(0xFF3B82F6),
        topLeft = Offset(cx - s * 1.1f, cy),
        size = Size(s * 2.2f, s * 0.6f)
    )
    // מנוע להבה
    drawCircle(Color(0xFFFF6B00), s * 0.35f, Offset(cx, cy + s * 0.9f))
    drawCircle(Color(0xFFFFD700), s * 0.2f, Offset(cx, cy + s * 0.9f))
}

fun DrawScope.drawAsteroid(x: Float, y: Float, w: Float, h: Float) {
    val cx = x + w / 2
    val cy = y + h / 2
    drawOval(Color(0xFF6B4226), topLeft = Offset(x, y), size = Size(w, h))
    drawOval(Color(0xFF8B5E3C), topLeft = Offset(x + w*0.1f, y + h*0.1f), size = Size(w*0.4f, h*0.35f))
    drawOval(Color(0xFF4A2E14), topLeft = Offset(x + w*0.55f, y + h*0.5f), size = Size(w*0.3f, h*0.3f))
}

// ─── מסך פתיחה ────────────────────────────────────
@Composable
fun MenuScreen(highScore: Int, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚀", fontSize = 28.sp, textAlign = TextAlign.Center)
        Text("SPACE\nDODGE",
            color = Color(0xFF60A5FA), fontSize = 16.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        if (highScore > 0) {
            Text("שיא: $highScore ⭐", color = Color(0xFFFFD700), fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A)),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("התחל!", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text("לחץ שמאל/ימין להזזה", color = Color(0xFF555555), fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

// ─── מסך מוות ─────────────────────────────────────
@Composable
fun DeadScreen(score: Int, highScore: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💥", fontSize = 24.sp)
        Text("נפגעת!", color = Color(0xFFFF6B6B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("ניקוד: $score", color = Color.White, fontSize = 13.sp)
        if (score >= highScore) {
            Text("🏆 שיא חדש!", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("שיא: $highScore", color = Color(0xFF888888), fontSize = 10.sp)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A)),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Text("שחק שוב!", color = Color.White, fontSize = 12.sp)
        }
    }
}

// ─── יצירת כוכבים אקראיים ─────────────────────────
fun generateStars(): List<Star> = List(STAR_COUNT) {
    Star(
        x = Random.nextFloat() * SCREEN_SIZE,
        y = Random.nextFloat() * SCREEN_SIZE,
        size = Random.nextFloat() * 1.5f + 0.5f,
        speed = Random.nextFloat() * 1.5f + 0.5f
    )
}
