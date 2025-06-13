package com.example.jobjet.ui.viewmodels

import com.example.jobjet.base.BaseViewModel
import com.example.jobjet.data.model.User
import com.example.jobjet.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel(
    private val repository: FirebaseRepository = FirebaseRepository.getInstance()
) : BaseViewModel<ProfileViewModel.State, ProfileViewModel.Event>() {

    data class State(
        val user: User? = null,
        val isEditing: Boolean = false,
        val editedName: String = "",
        val editedEmail: String = "",
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val isLoggedOut: Boolean = false
    )

    sealed class Event {
        object LoadProfile : Event()
        data class UpdateName(val name: String) : Event()
        data class UpdateEmail(val email: String) : Event()
        data class ToggleEditMode(val isEditing: Boolean) : Event()
        object SaveProfile : Event()
        object Logout : Event()
        object ClearError : Event()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        onEvent(Event.LoadProfile)
    }

    override fun onEvent(event: Event) {
        when (event) {
            Event.LoadProfile -> loadProfile()
            is Event.UpdateName -> {
                _state.update { it.copy(editedName = event.name) }
            }
            is Event.UpdateEmail -> {
                _state.update { it.copy(editedEmail = event.email) }
            }
            is Event.ToggleEditMode -> {
                _state.update { currentState ->
                    if (event.isEditing) {
                        currentState.copy(
                            isEditing = true,
                            editedName = currentState.user?.name ?: "",
                            editedEmail = currentState.user?.email ?: ""
                        )
                    } else {
                        currentState.copy(isEditing = false)
                    }
                }
            }
            Event.SaveProfile -> saveProfile()
            Event.Logout -> logout()
            Event.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun loadProfile() {
        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            repository.auth.currentUser?.let { user ->
                repository.getUserProfile(user.uid)
                    .onSuccess { profile ->
                        _state.update { 
                            it.copy(
                                user = profile,
                                editedName = profile.name,
                                editedEmail = profile.email
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update { 
                            it.copy(errorMessage = error.message ?: "Failed to load profile")
                        }
                    }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun saveProfile() {
        val currentState = _state.value
        val currentUser = currentState.user ?: return

        if (currentState.editedName.isBlank()) {
            _state.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val updatedUser = currentUser.copy(
                name = currentState.editedName,
                email = currentState.editedEmail
            )

            repository.updateUserProfile(updatedUser)
                .onSuccess { 
                    _state.update { 
                        it.copy(
                            user = updatedUser,
                            isEditing = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(errorMessage = error.message ?: "Failed to update profile")
                    }
                }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun logout() {
        launch {
            repository.auth.signOut()
            _state.update { it.copy(isLoggedOut = true) }
        }
    }

    fun canSaveProfile(): Boolean {
        val currentState = _state.value
        return currentState.isEditing &&
               currentState.editedName.isNotBlank() &&
               !currentState.isLoading
    }
}
