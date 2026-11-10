package com.example.myapplication

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface PostRepository {
    val postsFlow: StateFlow<List<LostFoundPost>>
    val currentUserFlow: StateFlow<UserProfile?>
    val allUsersFlow: StateFlow<List<UserProfile>>

    suspend fun loadInitialData()
    suspend fun addPost(post: LostFoundPost)
    suspend fun deletePost(id: String)
    suspend fun updatePostStatus(id: String, newStatus: String)

    suspend fun login(studentId: String, password: String): Boolean
    suspend fun resetPassword(studentId: String, newPassword: String): Boolean
    suspend fun logout()
    suspend fun saveUser(profile: UserProfile)
    suspend fun checkRegistrationAvailability(studentId: String, idCard: String): String?
    suspend fun updateUserContact(studentId: String, phone: String, campus: String)
}

class SqlitePostRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PostRepository {
    private val appContext = context.applicationContext
    private val dbHelper = PostDatabaseHelper(appContext)
    private val prefs = appContext.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

    private val _postsFlow = MutableStateFlow<List<LostFoundPost>>(emptyList())
    override val postsFlow: StateFlow<List<LostFoundPost>> = _postsFlow.asStateFlow()

    private val _currentUserFlow = MutableStateFlow<UserProfile?>(null)
    override val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow.asStateFlow()

    private val _allUsersFlow = MutableStateFlow<List<UserProfile>>(emptyList())
    override val allUsersFlow: StateFlow<List<UserProfile>> = _allUsersFlow.asStateFlow()

    override suspend fun loadInitialData() {
        withContext(ioDispatcher) {
            val list = dbHelper.getAllPosts()
            _postsFlow.value = list

            val users = dbHelper.getAllUserProfiles()
            _allUsersFlow.value = users

            val activeStudentId = prefs.getString("active_student_id", null)
            if (!activeStudentId.isNullOrBlank()) {
                val profile = dbHelper.getUserProfile(activeStudentId)
                _currentUserFlow.value = profile
            }
        }
    }

    override suspend fun addPost(post: LostFoundPost) {
        withContext(ioDispatcher) {
            dbHelper.insertPost(post)
            _postsFlow.value = dbHelper.getAllPosts()

            // Built-in matching engine: check against current user or registered users
            val current = _currentUserFlow.value
            if (current != null && MatchNotificationHelper.isPostMatchingUser(post, current)) {
                MatchNotificationHelper.sendMatchNotification(appContext, post, current)
            }
        }
    }

    override suspend fun deletePost(id: String) {
        withContext(ioDispatcher) {
            dbHelper.deletePost(id)
            _postsFlow.value = dbHelper.getAllPosts()
        }
    }

    override suspend fun updatePostStatus(id: String, newStatus: String) {
        withContext(ioDispatcher) {
            dbHelper.updateStatus(id, newStatus)
            _postsFlow.value = dbHelper.getAllPosts()
        }
    }

    override suspend fun login(studentId: String, password: String): Boolean {
        return withContext(ioDispatcher) {
            val profile = dbHelper.getUserProfile(studentId)
            if (profile != null && profile.password == password) {
                prefs.edit { putString("active_student_id", studentId) }
                _currentUserFlow.value = profile
                true
            } else {
                false
            }
        }
    }

    override suspend fun resetPassword(studentId: String, newPassword: String): Boolean {
        return withContext(ioDispatcher) {
            val updated = dbHelper.updateUserPassword(studentId, newPassword)
            if (updated > 0) {
                val profile = dbHelper.getUserProfile(studentId)
                if (profile != null) {
                    prefs.edit { putString("active_student_id", studentId) }
                    _currentUserFlow.value = profile
                }
                _allUsersFlow.value = dbHelper.getAllUserProfiles()
                true
            } else {
                false
            }
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            prefs.edit { remove("active_student_id") }
            _currentUserFlow.value = null
        }
    }

    override suspend fun saveUser(profile: UserProfile) {
        withContext(ioDispatcher) {
            dbHelper.saveUserProfile(profile)
            _allUsersFlow.value = dbHelper.getAllUserProfiles()
            // Auto login upon registration/save
            prefs.edit { putString("active_student_id", profile.studentId) }
            _currentUserFlow.value = profile
        }
    }

    override suspend fun checkRegistrationAvailability(studentId: String, idCard: String): String? {
        return withContext(ioDispatcher) {
            val cleanStudentId = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
            val cleanIdCard = idCard.replace(" ", "").trim().uppercase()

            if (dbHelper.isStudentIdRegistered(cleanStudentId)) {
                return@withContext "该学号（$cleanStudentId）已注册实名档案，不可重复绑定。请直接使用学号和应用密码登录。"
            }

            if (dbHelper.isIdCardBound(cleanIdCard)) {
                return@withContext "该身份证号已绑定其他学号档案（每张身份证仅允许绑定一次）。如需修改或申诉，请联系管理员提交证明材料。"
            }

            null
        }
    }

    override suspend fun updateUserContact(studentId: String, phone: String, campus: String) {
        withContext(ioDispatcher) {
            dbHelper.updateUserContact(studentId, phone, campus)
            val updated = dbHelper.getUserProfile(studentId)
            _currentUserFlow.value = updated
            _allUsersFlow.value = dbHelper.getAllUserProfiles()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SqlitePostRepository? = null

        fun getInstance(context: Context): SqlitePostRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SqlitePostRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
