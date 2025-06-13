package com.example.jobjet.ui.viewmodels

import com.example.jobjet.base.BaseViewModel
import com.example.jobjet.data.model.ChatMessage
import com.example.jobjet.data.model.ChatRoom
import com.example.jobjet.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel(
    private val repository: FirebaseRepository = FirebaseRepository.getInstance()
) : BaseViewModel<ChatViewModel.State, ChatViewModel.Event>() {

    data class State(
        val chatRooms: List<ChatRoom> = emptyList(),
        val selectedRoom: ChatRoom? = null,
        val messages: List<ChatMessage> = emptyList(),
        val newMessage: String = "",
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    )

    sealed class Event {
        object LoadChatRooms : Event()
        data class SelectRoom(val room: ChatRoom) : Event()
        data class UpdateNewMessage(val message: String) : Event()
        object SendMessage : Event()
        object ClearError : Event()
        object UnselectRoom : Event()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val currentUserId: String
        get() = repository.auth.currentUser?.uid ?: ""

    init {
        onEvent(Event.LoadChatRooms)
    }

    override fun onEvent(event: Event) {
        when (event) {
            Event.LoadChatRooms -> loadChatRooms()
            is Event.SelectRoom -> {
                selectRoom(event.room)
            }
            is Event.UpdateNewMessage -> {
                _state.update { it.copy(newMessage = event.message) }
            }
            Event.SendMessage -> sendMessage()
            Event.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            Event.UnselectRoom -> {
                _state.update { it.copy(selectedRoom = null, messages = emptyList()) }
            }
        }
    }

    private fun loadChatRooms() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            repository.getChatRooms(currentUserId)
                .collect { rooms ->
                    _state.update { it.copy(chatRooms = rooms) }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun selectRoom(room: ChatRoom) {
        launch {
            _state.update { it.copy(selectedRoom = room, isLoading = true) }

            repository.getChatMessages(room.id)
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun sendMessage() {
        val currentState = _state.value
        val room = currentState.selectedRoom ?: return
        val messageContent = currentState.newMessage.trim()

        if (messageContent.isBlank()) return

        launch {
            val message = ChatMessage(
                senderId = currentUserId,
                receiverId = room.participants.first { it != currentUserId },
                content = messageContent
            )

            repository.sendMessage(message)
                .onSuccess {
                    _state.update { it.copy(newMessage = "") }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(errorMessage = error.message ?: "Failed to send message")
                    }
                }
        }
    }

    fun getOtherParticipantId(room: ChatRoom): String {
        return room.participants.first { it != currentUserId }
    }

    fun canSendMessage(): Boolean {
        val currentState = _state.value
        return currentState.selectedRoom != null &&
               currentState.newMessage.isNotBlank() &&
               !currentState.isLoading
    }
}
