package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.navigation.FloatingGlassNav
import com.example.myapplication.ui.screens.DiscoverScreen
import com.example.myapplication.ui.screens.ProfileScreen
import com.example.myapplication.ui.screens.PublishScreen
import com.example.myapplication.ui.screens.RegisterScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selected by rememberSaveable { mutableStateOf("发现") }
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    val userPosts by viewModel.userPosts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val matchingPosts by viewModel.matchingPosts.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val pageStateHolder = rememberSaveableStateHolder()
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        if (isRegistering) {
            RegisterScreen(
                backdrop = backdrop,
                onBack = { isRegistering = false },
                onCheckAvailability = { studentId, idCard ->
                    viewModel.checkRegistrationAvailability(studentId, idCard)
                },
                onRegisterSuccess = { profile ->
                    viewModel.saveUser(profile)
                    isRegistering = false
                }
            )
        } else {
            pageStateHolder.SaveableStateProvider(selected) {
                when (selected) {
                    "发现" -> DiscoverScreen(
                        backdrop = backdrop,
                        userPosts = userPosts,
                        currentUser = currentUser,
                        onUpdatePostStatus = { id, status -> viewModel.updatePostStatus(id, status) },
                        onStartPublish = { selected = "发布" }
                    )
                    "发布" -> PublishScreen(
                        backdrop = backdrop,
                        currentUser = currentUser,
                        onLogin = { studentId, password, callback -> viewModel.login(studentId, password, callback) },
                        onResetPassword = { studentId, eduroamPass, newPass, callback ->
                            viewModel.resetPasswordViaEduroam(studentId, eduroamPass, newPass, callback)
                        },
                        onNavigateToRegister = { isRegistering = true },
                        onSubmit = { post ->
                            viewModel.publishPost(post)
                            selected = "发现"
                        }
                    )
                    else -> ProfileScreen(
                        backdrop = backdrop,
                        userPosts = userPosts,
                        currentUser = currentUser,
                        matchingPosts = matchingPosts,
                        allUsers = allUsers,
                        onLogin = { studentId, password, callback -> viewModel.login(studentId, password, callback) },
                        onResetPassword = { studentId, eduroamPass, newPass, callback ->
                            viewModel.resetPasswordViaEduroam(studentId, eduroamPass, newPass, callback)
                        },
                        onNavigateToRegister = { isRegistering = true },
                        onLogout = { viewModel.logout() },
                        onSaveUser = { profile ->
                            viewModel.updateUserContact(profile.studentId, profile.phone, profile.campus)
                        },
                        onUpdatePostStatus = { id, status -> viewModel.updatePostStatus(id, status) }
                    )
                }
            }
            FloatingGlassNav(
                backdrop = backdrop,
                selected = selected,
                onSelect = { selected = it },
                onCreate = { selected = "发布" },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 38.dp)
            )
        }
    }
}
