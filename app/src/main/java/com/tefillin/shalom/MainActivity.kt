package com.tefillin.shalom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay

// Game constants
const val PADDLE_WIDTH = 0.35f
const val PADDLE_HEIGHT = 0.04f
const val BALL_RADIUS = 0.03f
const val BRICK_ROWS = 4
const val BRICK_COLS = 5
const val BRICK_MARGIN = 0.02f

data class Brick(val col: Int, val row: Int, var alive: Boolean = true)
data class Ball(var x: Float, var y: Float, var vx: Float, var vy: Float)
data class Paddle(var x: Float)

enum class GameState { READY, PLAYING, WIN, LOSE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BrickBreakerGame() }
    }
}

@Composable
fun BrickBreakerGame() {
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var paddle by remember { mutableStateOf(Paddle(0.5f)) }
    var ball by remember { mutableStateOf(Ball(0.5f, 0.75f, 0.012f, -0.018f)) }
    var bricks by remember { mutableStateOf(
        (0 until BRICK_ROWS).flatMap { row ->
            (0 until BRICK_COLS).map { col -> Brick(col, row) }
        }
    )}

    fun resetGame() {
        paddle = Paddle(0.5f)
        ball = Ball(0.5f, 0.75f, 0.012f, -0.018f)
        bricks = (0 until BRICK_ROWS).flatMap { row ->
            (0 until BRICK_COLS).map { col -> Brick(col, row) }
        }
        score = 0
        gameState = GameState.PLAYING
    }

    // Game loop
    LaunchedEffect(gameState) {
        while (gameState == GameState.PLAYING) {
            delay(16L)

            var bx = ball.x + ball.vx
            var by = ball.y + ball.vy
            var vx = ball.vx
            var vy = ball.vy

            // Wall bounce
            if (bx < BALL_RADIUS || bx > 1f - BALL_RADIUS) vx = -vx
            if (by < BALL_RADIUS) vy = -vy

            // Paddle bounce
            val paddleLeft = paddle.x - PADDLE_WIDTH / 2
            val paddleRight = paddle.x + PADDLE_WIDTH / 2
            val paddleTop = 1f - PADDLE_HEIGHT - 0.05f
            if (by > paddleTop && by < paddleTop + PADDLE_HEIGHT &&
                bx > paddleLeft && bx < paddleRight) {
                vy = -Math.abs(vy)
                // Add angle based on hit position
                val hitPos = (bx - paddle.x) / (PADDLE_WIDTH / 2)
                vx = hitPos * 0.02f
            }

            // Brick collision
            val newBricks = bricks.toMutableList()
            val brickW = (1f - BRICK_MARGIN * (BRICK_COLS + 1)) / BRICK_COLS
            val brickH = 0.08f
            val brickStartY = 0.08f

            for (i in newBricks.indices) {
                val brick = newBricks[i]
                if (!brick.alive) continue
                val brickLeft = BRICK_MARGIN + brick.col * (brickW + BRICK_MARGIN)
                val brickTop = brickStartY + brick.row * (brickH + BRICK_MARGIN)
                val brickRight = brickLeft + brickW
                val brickBottom = brickTop + brickH

                if (bx > brickLeft && bx < brickRight &&
                    by > brickTop && by < brickBottom) {
                    newBricks[i] = brick.copy(alive = false)
                    score += 10
                    vy = -vy
                    break
                }
            }
            bricks = newBricks

            // Lose condition
            if (by > 1f) {
                gameState = GameState.LOSE
                return@LaunchedEffect
            }

            // Win condition
            if (bricks.none { it.alive }) {
                gameState = GameState.WIN
                return@LaunchedEffect
            }

            ball = Ball(bx, by, vx, vy)
        }
    }

    val bgColor = Color(0xFF0A0A2A)
    val brickColors = listOf(
        Color(0xFFEF4444), Color(0xFFF97316),
        Color(0xFFEAB308), Color(0xFF22C55E)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (gameState) {
            GameState.READY, GameState.WIN, GameState.LOSE -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (gameState) {
                            GameState.WIN -> "🎉 ניצחת!"
                            GameState.LOSE -> "💔 הפסדת"
                            else -> "🎮 Brick Breaker"
                        },
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (gameState != GameState.READY) {
                        Text(
                            text = "ניקוד: $score",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1E3A8A)
                        )
                    ) {
                        Text(
                            text = if (gameState == GameState.READY) "התחל" else "שחק שוב",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            GameState.PLAYING -> {
                // Score
                Text(
                    text = "$score",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                val newX = (paddle.x + dragAmount.x / size.width)
                                    .coerceIn(PADDLE_WIDTH / 2, 1f - PADDLE_WIDTH / 2)
                                paddle = Paddle(newX)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val brickW = (1f - BRICK_MARGIN * (BRICK_COLS + 1)) / BRICK_COLS
                    val brickH = 0.08f
                    val brickStartY = 0.08f

                    // Draw bricks
                    bricks.filter { it.alive }.forEach { brick ->
                        val left = (BRICK_MARGIN + brick.col * (brickW + BRICK_MARGIN)) * w
                        val top = (brickStartY + brick.row * (brickH + BRICK_MARGIN)) * h
                        drawRect(
                            color = brickColors[brick.row % brickColors.size],
                            topLeft = Offset(left + 1f, top + 1f),
                            size = Size(brickW * w - 2f, brickH * h - 2f)
                        )
                    }

                    // Draw paddle
                    val paddleLeft = (paddle.x - PADDLE_WIDTH / 2) * w
                    val paddleTop = (1f - PADDLE_HEIGHT - 0.05f) * h
                    drawRect(
                        color = Color(0xFF60A5FA),
                        topLeft = Offset(paddleLeft, paddleTop),
                        size = Size(PADDLE_WIDTH * w, PADDLE_HEIGHT * h)
                    )

                    // Draw ball
                    drawCircle(
                        color = Color.White,
                        radius = BALL_RADIUS * w,
                        center = Offset(ball.x * w, ball.y * h)
                    )
                }
            }
        }
    }
}
