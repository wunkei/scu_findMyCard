package com.example.myapplication.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.domain.validation.ValidationHelper
import com.example.myapplication.ui.components.*

@Composable
fun ProfileEditDialog(
    currentUser: UserProfile,
    allUsers: List<UserProfile> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    val isIdCardBound = currentUser.idCard.isNotBlank()
    val isPhoneBound = currentUser.phone.isNotBlank()

    var newIdCard by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var campus by remember { mutableStateOf(currentUser.campus.ifBlank { "江安校区" }) }
    var showAdminAppealDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("个人资料", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
                    }
                    TagPill("已认证", SuccessGreen)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF6F8FA),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("认证姓名", color = ScreenMuted, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(currentUser.name, color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("🔒", fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("川大学号", color = ScreenMuted, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(currentUser.studentId, color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("🔒", fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("居民身份证号", color = ScreenMuted, fontSize = 13.sp)
                            if (isIdCardBound) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val maskedId = ValidationHelper.maskDocumentNumber("身份证", currentUser.idCard)
                                    Text(maskedId, color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("🔒", fontSize = 11.sp)
                                }
                            } else {
                                TagPill("未绑定 · 待填写 ✏️", WarmOrange)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("联系手机号", color = ScreenMuted, fontSize = 13.sp)
                            if (isPhoneBound) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val maskedPhone = ValidationHelper.maskPhone(currentUser.phone)
                                    Text(maskedPhone, color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("🔒", fontSize = 11.sp)
                                }
                            } else {
                                TagPill("未绑定 · 待填写 ✏️", WarmOrange)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0F4F8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdminAppealDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "实名信息已绑定，不可自行修改",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScreenInk
                            )
                            Text(
                                "如需变更，请联系管理员提交证明",
                                fontSize = 11.sp,
                                color = BrandBlue
                            )
                        }
                        Text("申请修改 ›", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isIdCardBound) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FormTextField(
                            label = "居民身份证号 (补充绑定)",
                            value = newIdCard,
                            onValueChange = { newIdCard = it; errorMessage = "" },
                            placeholder = "请输入18位居民身份证号码"
                        )
                        Text(
                            "⚠️ 身份证号为防冒领核心实名凭据，一旦绑定即永久锁定，无法自行修改。",
                            color = WarmOrange,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                if (!isPhoneBound) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FormTextField(
                            label = "联系手机 (补充绑定)",
                            value = newPhone,
                            onValueChange = { newPhone = it; errorMessage = "" },
                            placeholder = "请输入11位中国大陆手机号码"
                        )
                        Text(
                            "⚠️ 手机号一旦绑定即作为实名联系凭据，保存后将永久锁定，无法自行修改。",
                            color = WarmOrange,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                if (isIdCardBound && isPhoneBound) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✅", fontSize = 14.sp)
                            Text(
                                "您的所有核心实名凭据与手机号均已完成绑定并安全锁定。",
                                color = SuccessGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

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

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 12.sp, lineHeight = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("取消", color = ScreenMuted)
                    }
                    Button(
                        onClick = {
                            var finalIdCard = currentUser.idCard
                            var finalPhone = currentUser.phone

                            if (!isIdCardBound && newIdCard.isNotBlank()) {
                                val cleanId = newIdCard.replace(" ", "").trim().uppercase()
                                val idErr = ValidationHelper.validateIdCard(cleanId, isOptional = false)
                                if (idErr != null) {
                                    errorMessage = idErr
                                    return@Button
                                }
                                if (allUsers.any { it.studentId != currentUser.studentId && it.idCard.equals(cleanId, ignoreCase = true) }) {
                                    errorMessage = "该身份证号已绑定其他学号档案（每张身份证仅限绑定一次）。"
                                    return@Button
                                }
                                finalIdCard = cleanId
                            }

                            if (!isPhoneBound && newPhone.isNotBlank()) {
                                val cleanPhone = newPhone.replace(" ", "").trim()
                                val phoneErr = ValidationHelper.validatePhone(cleanPhone, isOptional = false)
                                if (phoneErr != null) {
                                    errorMessage = phoneErr
                                    return@Button
                                }
                                finalPhone = cleanPhone
                            }

                            val updated = currentUser.copy(
                                idCard = finalIdCard,
                                phone = finalPhone,
                                campus = campus
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("保存修改", color = Color.White)
                    }
                }
            }
        }
    }

    if (showAdminAppealDialog) {
        AlertDialog(
            onDismissRequest = { showAdminAppealDialog = false },
            title = {
                Text("修改实名信息指引", color = ScreenInk, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "为保障校园卡信息安全与防冒领机制，数据库账号主码为学号，姓名与身份证号仅允许首次注册时实名绑定一次，绑定后个人无法自行更改。",
                        color = ScreenInk,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        "如因身份证号码登记错误等特殊原因确需更正，联系管理员并提供相关证明材料：",
                        color = ScreenInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F7FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("管理员联系方式：", fontSize = 11.sp, color = ScreenMuted, fontWeight = FontWeight.SemiBold)
                            Text("邮箱：card-admin@wunkei.com", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                            Text("地点：宇宙警察局", fontSize = 11.sp, color = ScreenMuted)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdminAppealDialog = false }) {
                    Text("我知道了", color = BrandBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White
        )
    }
}
