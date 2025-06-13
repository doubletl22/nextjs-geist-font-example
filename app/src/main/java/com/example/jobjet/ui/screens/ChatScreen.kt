package com.example.jobjet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.jobjet.data.model.ChatMessage
import com.example.jobjet.data.model.ChatRoom
import com.example.jobjet.data.repository.FirebaseRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val repository = remember { FirebaseRepository.getInstance() }
    var chatRooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var selectedRoom by remember { mutableStateOf<ChatRoom?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var newMessage by remember { mutableStateOf("") }

    // Current user ID (should be obtained from authentication)
    val currentUserId = FirebaseRepository.getInstance().auth.currentUser?.uid ?: ""

    // Fetch chat rooms
    LaunchedEffect(currentUserId) {
        repository.getChatRooms(currentUserId).collect { rooms ->
            chatRooms = rooms
        }
    }

    // Fetch messages when room is selected
    LaunchedEffect(selectedRoom) {
        selectedRoom?.let { room ->
            repository.getChatMessages(room.id).collect { messageList ->
                messages = messageList
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedRoom?.let { "Chat" } ?: "Messages") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedRoom != null) {
                            selectedRoom = null
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedRoom == null) {
            // Chat rooms list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatRooms) { room ->
                    ChatRoomItem(
                        room = room,
                        onClick = { selectedRoom = room }
                    )
                }
            }
        } else {
            // Chat messages
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Messages list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId
                        )
                    }
                }

                // Message input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            if (newMessage.isNotBlank()) {
                                // Send message
                                val message = ChatMessage(
                                    senderId = currentUserId,
                                    receiverId = selectedRoom!!.participants.first { it != currentUserId },
                                    content = newMessage
                                )
                                repository.sendMessage(message)
                                newMessage = ""
                            }
                        },
                        enabled = newMessage.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRoomItem(
    room: ChatRoom,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chat Room ${room.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = room.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(room.lastMessageTimestamp.toDate()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isCurrentUser) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isCurrentUser) 
                    MaterialTheme.colorScheme.onPrimary
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(message.timestamp.toDate()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
