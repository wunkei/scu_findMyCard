package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.myapplication.data.model.LostFoundPost
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.domain.auth.EduroamResult
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.dialogs.AuthDialog
import com.example.myapplication.ui.dialogs.ProfileEditDialog
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun ProfileScreen(
    backdrop: LayerBackdrop,
    userPosts: List<LostFoundPost>,
    currentUser: UserProfile?,
    matchingPosts: List<LostFoundPost>,
    allUsers: List<UserProfile>,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPassword: (String, String, String, (EduroamResult) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogout: () -> Unit,
    onSaveUser: (UserProfile) -> Unit,
    onUpdatePostStatus: (String, String) -> Unit = { _, _ -> }
) {
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var dialogTarget by remember { mutableStateOf<String?>(null) }
    val openPosts = userPosts.filter { it.status !in setOf("已归还", "已归还失主", "已解决") }

    PageShell(backdrop) {
        ProfileCard(
            currentUser = currentUser,
            onClick = {
                if (currentUser == null) {
                    showAuthDialog = true
                } else {
                    showProfileEditDialog = true
                }
            }
        )

        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatValue(
                    label = "我的发布",
                    value = if (currentUser == null) "--" else userPosts.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentUser == null) showAuthDialog = true else dialogTarget = "我的拾卡"
                        }
                )
                StatValue(
                    label = "智能关联",
                    value = if (currentUser == null) "--" else matchingPosts.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentUser == null) showAuthDialog = true else dialogTarget = "我的认领"
                        }
                )
                StatValue(
                    label = "处理中",
                    value = if (currentUser == null) "--" else openPosts.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentUser == null) showAuthDialog = true
                        }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("我的记录", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
            RecordRow(
                symbol = "✓",
                title = "我的拾卡",
                subtitle = if (currentUser == null) "登录后查看我登记的拾获信息" else "查看我登记的拾获信息",
                count = if (currentUser == null) 0 else userPosts.count { it.type == "拾卡" },
                tint = SuccessGreen,
                onClick = {
                    if (currentUser == null) {
                        showAuthDialog = true
                    } else {
                        dialogTarget = "我的拾卡"
                    }
                }
            )
            RecordRow(
                symbol = "↔",
                title = "智能关联认领",
                subtitle = when {
                    currentUser == null -> "登录后自动比对失卡与认领进度"
                    matchingPosts.isNotEmpty() -> "🎉 发现 ${matchingPosts.size} 张可能属于您的卡片！"
                    else -> "暂无关联卡片，发布后将自动比对"
                },
                count = if (currentUser == null) 0 else matchingPosts.size,
                tint = if (matchingPosts.isNotEmpty()) WarmOrange else BrandBlue,
                onClick = {
                    if (currentUser == null) {
                        showAuthDialog = true
                    } else {
                        dialogTarget = "我的认领"
                    }
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("其他", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
            SettingRow("个人资料") {
                if (currentUser == null) {
                    showAuthDialog = true
                } else {
                    showProfileEditDialog = true
                }
            }
            SettingRow("隐私与安全") { dialogTarget = "隐私与安全" }
            SettingRow("关于川大寻卡") { dialogTarget = "关于川大寻卡" }
            if (currentUser != null) {
                SettingRow("退出当前账号") { dialogTarget = "退出登录" }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onLogin = onLogin,
            onResetPassword = onResetPassword,
            onNavigateToRegister = onNavigateToRegister
        )
    }

    if (showProfileEditDialog && currentUser != null) {
        ProfileEditDialog(
            currentUser = currentUser,
            onDismiss = { showProfileEditDialog = false },
            onSave = onSaveUser
        )
    }

    dialogTarget?.let { target ->
        when (target) {
            "我的拾卡" -> {
                val myPosts = userPosts.filter { it.type == "拾卡" }
                AlertDialog(
                    onDismissRequest = { dialogTarget = null },
                    title = { Text("我的拾卡记录", color = ScreenInk, fontWeight = FontWeight.Bold) },
                    text = {
                        if (myPosts.isEmpty()) {
                            Text("目前还没有拾卡记录。您可以在“发布”页面登记拾获的卡片。", color = ScreenMuted)
                        } else {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                myPosts.forEach { post ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF7F9FB),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(post.title, color = ScreenInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                TagPill(post.status, if (post.status.contains("已归还")) SuccessGreen else BrandBlue)
                                            }
                                            if (post.personName.isNotBlank() || post.documentNumber.isNotBlank()) {
                                                Text("持卡人：${post.personName} · ${post.documentNumber}", color = ScreenInk, fontSize = 12.sp)
                                            }
                                            Text("${post.campus} · ${post.location}", color = ScreenMuted, fontSize = 12.sp)
                                            Text("拾获时间：${post.date}", color = ScreenMuted, fontSize = 11.sp)
                                            if (post.latitude != null && post.longitude != null) {
                                                Text("📍 坐标：北纬 ${"%.4f".format(post.latitude)}°, 东经 ${"%.4f".format(post.longitude)}°", color = ScreenMuted, fontSize = 11.sp)
                                            }
                                            if (post.imageUri.isNotBlank()) {
                                                Text("📷 已附带现场照片", color = BrandBlue, fontSize = 11.sp)
                                            }

                                            if (!post.status.contains("已归还")) {
                                                Button(
                                                    onClick = {
                                                        onUpdatePostStatus(post.id, "已归还失主")
                                                        dialogTarget = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    Text("失主已领回 · 标记已归还", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { dialogTarget = null }) { Text("关闭", color = BrandBlue) }
                    },
                    containerColor = Color.White
                )
            }
            "我的认领" -> {
                AlertDialog(
                    onDismissRequest = { dialogTarget = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("智能关联认领", color = ScreenInk, fontWeight = FontWeight.Bold)
                            if (matchingPosts.isNotEmpty()) {
                                TagPill("${matchingPosts.size} 条命中", WarmOrange)
                            }
                        }
                    },
                    text = {
                        if (matchingPosts.isEmpty()) {
                            Text(
                                if (currentUser != null) {
                                    "系统正在自动为您监测新发布的拾卡信息。\n当前暂无与您的学号【${currentUser.studentId}】${if (currentUser.idCard.isNotBlank()) "或身份证号" else ""}完全匹配的卡片。"
                                } else {
                                    "请先登录实名档案，系统将自动比对失卡。"
                                },
                                color = ScreenMuted,
                                lineHeight = 18.sp
                            )
                        } else {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "以下卡片证件号码与您的实名档案完全吻合：",
                                    fontSize = 12.sp,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                                matchingPosts.forEach { post ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF9F5),
                                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(post.title, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                TagPill(post.status, WarmOrange)
                                            }
                                            Text("卡片姓名：${post.personName} · 号码：${post.documentNumber}", color = ScreenInk, fontSize = 12.sp)
                                            Text("拾获地点：${post.campus} · ${post.location}", color = ScreenMuted, fontSize = 12.sp)
                                            Text("拾获时间：${post.date}", color = ScreenMuted, fontSize = 11.sp)

                                            if (!post.status.contains("已归还")) {
                                                Button(
                                                    onClick = {
                                                        onUpdatePostStatus(post.id, "已归还失主")
                                                        dialogTarget = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    Text("确认这是我的卡 · 标记已取回", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { dialogTarget = null }) { Text("关闭", color = BrandBlue) }
                    },
                    containerColor = Color.White
                )
            }
            "退出登录" -> {
                AlertDialog(
                    onDismissRequest = { dialogTarget = null },
                    title = { Text("确认退出登录", color = ScreenInk, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "确定要退出当前账号【${currentUser?.studentId}】的登录吗？退出后将切换为未登录游客状态，所有个人信息将全部留空。",
                            color = ScreenMuted,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onLogout()
                            dialogTarget = null
                        }) {
                            Text("退出登录", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogTarget = null }) { Text("取消", color = ScreenMuted) }
                    },
                    containerColor = Color.White
                )
            }
            else -> {
                AlertDialog(
                    onDismissRequest = { dialogTarget = null },
                    title = { Text(target, color = ScreenInk, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = when (target) {
                                "隐私与安全" -> "1. 校园网密码仅在实名认证时单次提交至验证接口，应用本地绝不保存密码。\n2. 完整证件号码与姓名存储于本地数据库用于匹配，公开信息广场脱敏展示。\n3. 请仅在您的个人可信设备上登录使用。"
                                "关于川大寻卡" -> "川大寻卡致力于帮助四川大学失主与拾获者建立快捷、可信的联系。\n当前版本为本地交互与认证原型系统。"
                                else -> "功能入口已就绪。"
                            },
                            color = ScreenMuted,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { dialogTarget = null }) { Text("我知道了", color = BrandBlue) }
                    },
                    containerColor = Color.White
                )
            }
        }
    }
}
