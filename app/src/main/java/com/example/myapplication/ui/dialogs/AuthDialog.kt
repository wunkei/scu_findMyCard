package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.domain.auth.EduroamResult
import com.example.myapplication.domain.validation.ValidationHelper
import com.example.myapplication.ui.components.*

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPassword: (String, String, String, (EduroamResult) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var isForgotPassword by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf("") }
    var appPassword by remember { mutableStateOf("") }
    var eduroamPassword by remember { mutableStateOf("") }
    var newAppPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isForgotPassword) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("登录", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
                    }

                    FormTextField(
                        label = "学号",
                        value = studentId,
                        onValueChange = { studentId = it; errorMessage = "" },
                        placeholder = "输入学号"
                    )

                    FormTextField(
                        label = "登录密码",
                        value = appPassword,
                        onValueChange = { appPassword = it; errorMessage = "" },
                        placeholder = "输入登录密码",
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            Text(
                                text = if (isPasswordVisible) "隐藏" else "显示",
                                color = BrandBlue,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { isPasswordVisible = !isPasswordVisible }
                                    .padding(4.dp)
                            )
                        }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "忘记密码？",
                            color = BrandBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    isForgotPassword = true
                                    errorMessage = ""
                                    eduroamPassword = ""
                                    newAppPassword = ""
                                }
                                .padding(vertical = 4.dp)
                        )
                    }

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 12.sp)
                    }

                    PrimaryActionButton(
                        text = "登录",
                        onClick = {
                            val studentIdErr = ValidationHelper.validateStudentId(studentId)
                            if (studentIdErr != null) {
                                errorMessage = studentIdErr
                                return@PrimaryActionButton
                            }
                            if (appPassword.isBlank()) {
                                errorMessage = "请输入登录密码"
                                return@PrimaryActionButton
                            }
                            val cleanStudentId = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
                            onLogin(cleanStudentId, appPassword) { success ->
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMessage = "学号或登录密码错误，请重新核对"
                                }
                            }
                        }
                    )

                    TextButton(
                        onClick = {
                            onDismiss()
                            onNavigateToRegister()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("还没有注册账号？点击注册", color = BrandBlue, fontSize = 12.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isForgotPassword = false; errorMessage = "" }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text("‹", fontSize = 20.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                        Text("返回账号密码登录", fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("找回密码", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
                        Text(
                            "核验您的学号与校园网密码后，即可直接重设应用登录密码",
                            fontSize = 12.sp,
                            color = ScreenMuted,
                            lineHeight = 17.sp
                        )
                    }

                    FormTextField(
                        label = "学号 *",
                        value = studentId,
                        onValueChange = { studentId = it; errorMessage = "" },
                        placeholder = "输入您的学号"
                    )

                    FormTextField(
                        label = "校园网密码 * ",
                        value = eduroamPassword,
                        onValueChange = { eduroamPassword = it; errorMessage = "" },
                        placeholder = "输入校园网密码",
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            Text(
                                text = if (isPasswordVisible) "隐藏" else "显示",
                                color = BrandBlue,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { isPasswordVisible = !isPasswordVisible }
                                    .padding(4.dp)
                            )
                        }
                    )

                    FormTextField(
                        label = "新登录密码 *",
                        value = newAppPassword,
                        onValueChange = { newAppPassword = it; errorMessage = "" },
                        placeholder = "输入新设置的登录密码",
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3F7FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 14.sp)
                            Text(
                                "校园网密码仅用于单次核验，通过后将重置为您指定的新密码并自动登录。",
                                fontSize = 11.sp,
                                color = ScreenMuted,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 12.sp)
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                                Text("正在验证校园网密码…", color = ScreenMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    PrimaryActionButton(
                        text = if (isLoading) "正在验证并重置…" else "验证校园网密码并重设",
                        onClick = {
                            if (isLoading) return@PrimaryActionButton
                            val studentIdErr = ValidationHelper.validateStudentId(studentId)
                            if (studentIdErr != null) {
                                errorMessage = studentIdErr
                                return@PrimaryActionButton
                            }
                            if (eduroamPassword.isBlank()) {
                                errorMessage = "请输入校园网密码进行核验"
                                return@PrimaryActionButton
                            }
                            if (newAppPassword.isBlank()) {
                                errorMessage = "请输入新登录密码"
                                return@PrimaryActionButton
                            }

                            isLoading = true
                            errorMessage = ""
                            val cleanStudentId = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
                            onResetPassword(cleanStudentId, eduroamPassword, newAppPassword) { result ->
                                isLoading = false
                                when (result) {
                                    is EduroamResult.Success -> {
                                        onDismiss()
                                    }
                                    is EduroamResult.Failed -> {
                                        errorMessage = result.message
                                    }
                                    is EduroamResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
