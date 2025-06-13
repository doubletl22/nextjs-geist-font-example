package com.example.jobjet.data.repository

import com.example.jobjet.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Authentication
    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String, name: String, role: UserRole): Result<Unit> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = User(
            id = authResult.user?.uid ?: "",
            email = email,
            name = name,
            role = role
        )
        firestore.collection("users").document(user.id).set(user).await()
    }

    // Jobs
    suspend fun postJob(job: Job): Result<Unit> = runCatching {
        firestore.collection("jobs").add(job).await()
    }

    fun getJobs(): Flow<List<Job>> = flow {
        val snapshot = firestore.collection("jobs")
            .orderBy("postedDate", Query.Direction.DESCENDING)
            .get()
            .await()
        emit(snapshot.toObjects(Job::class.java))
    }

    suspend fun getJobById(jobId: String): Result<Job> = runCatching {
        firestore.collection("jobs").document(jobId).get().await().toObject(Job::class.java)
            ?: throw Exception("Job not found")
    }

    // Chat
    suspend fun sendMessage(message: ChatMessage): Result<Unit> = runCatching {
        firestore.collection("messages").add(message).await()
    }

    fun getChatMessages(roomId: String): Flow<List<ChatMessage>> = flow {
        val snapshot = firestore.collection("messages")
            .whereEqualTo("roomId", roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        emit(snapshot.toObjects(ChatMessage::class.java))
    }

    fun getChatRooms(userId: String): Flow<List<ChatRoom>> = flow {
        val snapshot = firestore.collection("chatRooms")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        emit(snapshot.toObjects(ChatRoom::class.java))
    }

    // User Profile
    suspend fun updateUserProfile(user: User): Result<Unit> = runCatching {
        firestore.collection("users").document(user.id).set(user).await()
    }

    suspend fun getUserProfile(userId: String): Result<User> = runCatching {
        firestore.collection("users").document(userId).get().await().toObject(User::class.java)
            ?: throw Exception("User not found")
    }

    companion object {
        private var instance: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository {
            return instance ?: FirebaseRepository().also { instance = it }
        }
    }
}
