package com.watchchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

// =============================================
// שנה את הכתובת לאחר העלאה ל-Render
// =============================================
const val SERVER_URL = "wss://snv-9lmr.onrender.com"

enum class Screen { ROOMS, CHAT, NEW_ROOM, EMOJI_PICKER }

data class ChatMessage(
    val from: String,
    val text: String,
    val time: String,
    val isSystem: Boolean = false,
    val isMine: Boolean = false
)

data class Room(val name: String, val count: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchChatApp()
        }
    }
}

@Composable
fun WatchChatApp() {
    var screen by remember { mutableStateOf(Screen.ROOMS) }
    var nickname by remember { mutableStateOf("") }
    var currentRoom by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf(listOf<Room>()) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var wsClient by remember { mutableStateOf<WebSocketClient?>(null) }
    var connected by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Connect to WebSocket
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = object : WebSocketClient(URI(SERVER_URL)) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        connected = true
                    }

                    override fun onMessage(message: String?) {
                        message ?: return
                        try {
                            val json = JSONObject(message)
                            when (json.getString("type")) {
                                "welcome" -> {
                                    nickname = json.getString("nickname")
                                    val roomsArr = json.getJSONArray("rooms")
                                    rooms = parseRooms(roomsArr)
                                }
                                "room_list" -> {
                                    val roomsArr = json.getJSONArray("rooms")
                                    rooms = parseRooms(roomsArr)
                                }
                                "joined" -> {
                                    currentRoom = json.getString("room")
                                    messages = listOf()
                                    screen = Screen.CHAT
                                }
                                "message" -> {
                                    val from = json.getString("from")
                                    val text = json.getString("text")
                                    val time = json.getString("time")
                                    messages = messages + ChatMessage(
                                        from = from,
                                        text = text,
                                        time = time,
                                        isMine = from == nickname
                                    )
                                }
                                "system" -> {
                                    messages = messages + ChatMessage(
                                        from = "מערכת",
                                        text = json.getString("text"),
                                        time = "",
                                        isSystem = true
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        connected = false
                    }

                    override fun onError(ex: Exception?) {
                        connected = false
                    }
                }
                client.connect()
                wsClient = client
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String) {
        wsClient?.send(JSONObject().apply {
            put("type", "message")
            put("text", text)
        }.toString())
    }

    fun joinRoom(roomName: String) {
        wsClient?.send(JSONObject().apply {
            put("type", "join")
            put("room", roomName)
        }.toString())
    }

    fun createRoom(roomName: String) {
        wsClient?.send(JSONObject().apply {
            put("type", "create_room")
            put("room", roomName)
        }.toString())
    }

    fun refreshRooms() {
        wsClient?.send(JSONObject().apply {
            put("type", "get_rooms")
        }.toString())
    }

    val bgColor = Color(0xFF0A0A1A)

    when (screen) {
        Screen.ROOMS -> RoomsScreen(
            rooms = rooms,
            nickname = nickname,
            connected = connected,
            onJoin = { joinRoom(it) },
            onNewRoom = { screen = Screen.NEW_ROOM },
            onRefresh = { refreshRooms() }
        )
        Screen.CHAT -> ChatScreen(
            messages = messages,
            roomName = currentRoom,
            nickname = nickname,
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    sendMessage(inputText)
                    inputText = ""
                }
            },
            onBack = {
                screen = Screen.ROOMS
                refreshRooms()
            },
            onEmojiPicker = { screen = Screen.EMOJI_PICKER }
        )
        Screen.NEW_ROOM -> NewRoomScreen(
            onCreate = { name ->
                createRoom(name)
                joinRoom(name)
            },
            onBack = { screen = Screen.ROOMS }
        )
        Screen.EMOJI_PICKER -> EmojiPickerScreen(
            onPick = { emoji ->
                inputText += emoji
                screen = Screen.CHAT
            },
            onBack = { screen = Screen.CHAT }
        )
    }
}

fun parseRooms(arr: JSONArray): List<Room> {
    val list = mutableListOf<Room>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(Room(obj.getString("name"), obj.getInt("count")))
    }
    return list
}

@Composable
fun RoomsScreen(
    rooms: List<Room>,
    nickname: String,
    connected: Boolean,
    onJoin: (String) -> Unit,
    onNewRoom: () -> Unit,
    onRefresh: () -> Unit
) {
    val bgColor = Color(0xFF0A0A2E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "💬 WatchChat",
                color = Color(0xFFFFD700),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (connected) "✅ $nickname" else "❌ לא מחובר",
                color = if (connected) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (rooms.isEmpty()) {
                Text(
                    text = "אין חדרים עדיין",
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                rooms.forEach { room ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(Color(0xFF1A1A4A))
                            .clickable { onJoin(room.name) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = room.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "👥 ${room.count}",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons
            Button(
                onClick = onNewRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A))
            ) {
                Text(text = "+ חדר חדש", color = Color.White, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A2A3A))
            ) {
                Text(text = "🔄 רענן", color = Color(0xFF60A5FA), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    roomName: String,
    nickname: String,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onEmojiPicker: () -> Unit
) {
    val bgColor = Color(0xFF0A0A1A)
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A2E))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◀",
                    color = Color(0xFF60A5FA),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() }
                )
                Text(
                    text = roomName,
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(messages) { msg ->
                    if (msg.isSystem) {
                        Text(
                            text = msg.text,
                            color = Color(0xFF888888),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
                        ) {
                            if (!msg.isMine) {
                                Text(
                                    text = msg.from,
                                    color = Color(0xFF60A5FA),
                                    fontSize = 9.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (msg.isMine) Color(0xFF1E3A8A) else Color(0xFF1A1A3A)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            if (msg.time.isNotEmpty()) {
                                Text(
                                    text = msg.time,
                                    color = Color(0xFF555555),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A2E))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Emoji button
                Button(
                    onClick = onEmojiPicker,
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A1A4A))
                ) {
                    Text(text = "😊", fontSize = 12.sp)
                }

                // Text input (clickable area showing current text)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(Color(0xFF1A1A3A))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (inputText.isEmpty()) "הקלד..." else inputText,
                        color = if (inputText.isEmpty()) Color(0xFF555555) else Color.White,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Send button
                Button(
                    onClick = onSend,
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A))
                ) {
                    Text(text = "▶", color = Color.White, fontSize = 10.sp)
                }
            }

            // Quick messages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("👍", "אוקי", "בדרך").forEach { quick ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1A1A3A))
                            .clickable {
                                onInputChange(quick)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(text = quick, color = Color(0xFF60A5FA), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NewRoomScreen(onCreate: (String) -> Unit, onBack: () -> Unit) {
    val bgColor = Color(0xFF0A0A2E)
    var selected by remember { mutableStateOf("") }

    val suggestions = listOf("כללי", "משפחה", "עבודה", "חברים", "דחוף", "ספורט")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("◀", color = Color(0xFF60A5FA), fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() })
                Text("חדר חדש", color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("בחר שם:", color = Color(0xFF888888), fontSize = 11.sp)

            Spacer(modifier = Modifier.height(6.dp))

            suggestions.forEach { name ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(if (selected == name) Color(0xFF1E3A8A) else Color(0xFF1A1A3A))
                        .clickable { selected = name }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = name, color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selected.isNotEmpty()) {
                Button(
                    onClick = { onCreate(selected) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A8A))
                ) {
                    Text(text = "צור: $selected", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmojiPickerScreen(onPick: (String) -> Unit, onBack: () -> Unit) {
    val bgColor = Color(0xFF0A0A2E)
    val emojis = listOf(
        "😊", "😂", "❤️", "👍", "👎", "🙏", "🔥", "✅", "❌", "⚠️",
        "😅", "😭", "😤", "🤔", "😴", "🤩", "😎", "🥳", "😡", "🤗",
        "👋", "🤝", "💪", "🏃", "🚗", "🏠", "📞", "⏰", "💊", "🆘"
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
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("◀", color = Color(0xFF60A5FA), fontSize = 14.sp,
                    modifier = Modifier.clickable { onBack() })
                Text("בחר אמוג'י", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of emojis - 5 per row
            emojis.chunked(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onPick(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
