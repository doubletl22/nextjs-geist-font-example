package com.example.jobjet.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val profilePicUrl: String = "",
    val role: UserRole = UserRole.JOB_SEEKER
)

enum class UserRole {
    JOB_SEEKER,
    EMPLOYER
}

data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val description: String = "",
    val location: String = "",
    val salary: String = "",
    val requirements: List<String> = emptyList(),
    val postedBy: String = "",
    val postedDate: Timestamp = Timestamp.now(),
    val type: JobType = JobType.FULL_TIME
)

enum class JobType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    FREELANCE
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now()
)
