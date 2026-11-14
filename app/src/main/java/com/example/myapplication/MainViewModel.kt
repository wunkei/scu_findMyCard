package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.LostFoundPost
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.data.repository.PostRepository
import com.example.myapplication.data.repository.SqlitePostRepository
import com.example.myapplication.domain.auth.EduroamAuthHelper
import com.example.myapplication.domain.auth.EduroamResult
import com.example.myapplication.domain.matching.MatchNotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = SqlitePostRepository.getInstance(application)

    val userPosts: StateFlow<List<LostFoundPost>> = repository.postsFlow
    val currentUser: StateFlow<UserProfile?> = repository.currentUserFlow
    val allUsers: StateFlow<List<UserProfile>> = repository.allUsersFlow

    val matchingPosts: StateFlow<List<LostFoundPost>> = combine(
        repository.postsFlow,
        repository.currentUserFlow
    ) { posts, user ->
        if (user == null) {
            emptyList()
        } else {
            posts.filter { MatchNotificationHelper.isPostMatchingUser(it, user) }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            repository.loadInitialData()
        }
    }

    fun publishPost(post: LostFoundPost) {
        viewModelScope.launch {
            repository.addPost(post)
        }
    }

    fun deletePost(id: String) {
        viewModelScope.launch {
            repository.deletePost(id)
        }
    }

    fun updatePostStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            repository.updatePostStatus(id, newStatus)
        }
    }

    fun login(studentId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.login(studentId, password)
            onResult(success)
        }
    }

    fun resetPasswordViaEduroam(
        studentId: String,
        eduroamPass: String,
        newAppPass: String,
        onResult: (EduroamResult) -> Unit
    ) {
        viewModelScope.launch {
            val eduroamResult = EduroamAuthHelper.verifyEduroamAccount(
                username = studentId,
                password = eduroamPass,
                context = getApplication()
            )
            if (eduroamResult is EduroamResult.Success) {
                repository.resetPassword(studentId, newAppPass)
            }
            onResult(eduroamResult)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    suspend fun checkRegistrationAvailability(studentId: String, idCard: String): String? {
        return repository.checkRegistrationAvailability(studentId, idCard)
    }

    fun updateUserContact(studentId: String, phone: String, campus: String) {
        viewModelScope.launch {
            repository.updateUserContact(studentId, phone, campus)
        }
    }

    fun saveUser(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUser(profile)
        }
    }
}
