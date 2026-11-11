package com.example.myapplication.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.domain.auth.EduroamAuthHelper
import com.example.myapplication.domain.auth.EduroamResult
import com.example.myapplication.domain.validation.ValidationHelper
import com.example.myapplication.ui.components.*
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onCheckAvailability: suspend (String, String) -> String? = { _, _ -> null },
    onRegisterSuccess: (UserProfile) -> Unit
) {
    BackHandler(onBack = onBack)

    val coroutineScope = rememberCoroutineScope()
    var studentId by rememberSaveable { mutableStateOf("") }
    var eduroamPassword by rememberSaveable { mutableStateOf("") }
    var appPassword by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var idCard by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("江安校区") }
    var isEduroamPasswordVisible by remember { mutableStateOf(false) }
    var isAppPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    PageShell(backdrop) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Text("‹", fontSize = 24.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
            Text("返回", fontSize = 14.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("注册", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
        }

        InfoCard(Modifier.fillMaxWidth()) {
            FormTextField(
                label = "真实姓名 * (必填)",
                value = name,
                onValueChange = { name = it; errorMessage = "" },
                placeholder = "例如：张三"
            )

            FormTextField(
                label = "学号 * (必填)",
                value = studentId,
                onValueChange = { studentId = it; errorMessage = "" },
                placeholder = "例如：2024141460000"
            )

            FormTextField(
                label = "校园网密码 * (仅单次核验)",
                value = eduroamPassword,
                onValueChange = { eduroamPassword = it; errorMessage = "" },
                placeholder = "输入校园网密码",
                visualTransformation = if (isEduroamPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    Text(
                        text = if (isEduroamPasswordVisible) "隐藏" else "显示",
                        color = BrandBlue,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isEduroamPasswordVisible = !isEduroamPasswordVisible }
                            .padding(4.dp)
                    )
                }
            )

            FormTextField(
                label = "设置登录密码 (选填)",
                value = appPassword,
                onValueChange = { appPassword = it; errorMessage = "" },
                placeholder = "留空则默认使用上述校园网密码",
                visualTransformation = if (isAppPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    Text(
                        text = if (isAppPasswordVisible) "隐藏" else "显示",
                        color = BrandBlue,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isAppPasswordVisible = !isAppPasswordVisible }
                            .padding(4.dp)
                    )
                }
            )
        }

        InfoCard(Modifier.fillMaxWidth()) {
            FormTextField(
                label = "居民身份证号 (选填)",
                value = idCard,
                onValueChange = { idCard = it; errorMessage = "" },
                placeholder = "选填，用于卡片自动精准比对（若填须完整18位）"
            )

            FormTextField(
                label = "联系手机 (选填)",
                value = phone,
                onValueChange = { phone = it; errorMessage = "" },
                placeholder = "方便拾获者联系归还"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("常驻校区", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("江安校区", "望江校区", "华西校区").forEach { campusName ->
                        SelectChip(campusName, selected = campus == campusName) {
                            campus = campusName
                        }
                    }
                }
            }
        }


        if (errorMessage.isNotBlank()) {
            Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 12.sp, lineHeight = 16.sp)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                    Text("正在连接验证服务器…", color = ScreenMuted, fontSize = 12.sp)
                }
            }
        }

        PrimaryActionButton(
            text = if (isLoading) "正在验证中…" else "完成验证并注册",
            onClick = {
                if (isLoading) return@PrimaryActionButton
                val nameErr = ValidationHelper.validateName(name)
                if (nameErr != null) {
                    errorMessage = nameErr
                    return@PrimaryActionButton
                }
                val studentIdErr = ValidationHelper.validateStudentId(studentId)
                if (studentIdErr != null) {
                    errorMessage = studentIdErr
                    return@PrimaryActionButton
                }
                if (eduroamPassword.isBlank()) {
                    errorMessage = "请填写校园网密码以完成身份验证"
                    return@PrimaryActionButton
                }
                val idCardErr = ValidationHelper.validateIdCard(idCard, isOptional = true)
                if (idCardErr != null) {
                    errorMessage = idCardErr
                    return@PrimaryActionButton
                }
                val phoneErr = ValidationHelper.validatePhone(phone, isOptional = true)
                if (phoneErr != null) {
                    errorMessage = phoneErr
                    return@PrimaryActionButton
                }

                isLoading = true
                errorMessage = ""
                coroutineScope.launch {
                    val availabilityErr = onCheckAvailability(studentId, idCard)
                    if (availabilityErr != null) {
                        isLoading = false
                        errorMessage = availabilityErr
                        return@launch
                    }

                    val result = EduroamAuthHelper.verifyEduroamAccount(studentId, eduroamPassword)
                    isLoading = false
                    when (result) {
                        is EduroamResult.Success -> {
                            val cleanStudentId = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
                            val cleanIdCard = idCard.replace(" ", "").trim().uppercase()
                            val cleanPhone = phone.replace(" ", "").trim()
                            val finalAppPassword = if (appPassword.isNotBlank()) appPassword.trim() else eduroamPassword.trim()
                            val newProfile = UserProfile(
                                studentId = cleanStudentId,
                                name = name.trim(),
                                password = finalAppPassword,
                                idCard = cleanIdCard,
                                phone = cleanPhone,
                                campus = campus,
                                isVerified = true
                            )
                            onRegisterSuccess(newProfile)
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
