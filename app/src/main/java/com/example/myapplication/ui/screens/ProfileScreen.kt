package com.example.myapplication.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val context = LocalContext.current
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var dialogTarget by remember { mutableStateOf<String?>(null) }
    var selectedDetailPost by remember { mutableStateOf<LostFoundPost?>(null) }
    var viewingPhotoUri by remember { mutableStateOf<String?>(null) }
    val unclaimedPosts = userPosts.filter { !it.status.contains("已归还") && !it.status.contains("认领") }
    val claimedPosts = userPosts.filter { it.status.contains("已归还") || it.status.contains("认领") }

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
                            if (currentUser == null) showAuthDialog = true else dialogTarget = "我的发布"
                        }
                )
                StatValue(
                    label = "未认领",
                    value = if (currentUser == null) "--" else unclaimedPosts.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentUser == null) showAuthDialog = true else dialogTarget = "未认领"
                        }
                )
                StatValue(
                    label = "已认领",
                    value = if (currentUser == null) "--" else claimedPosts.size.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentUser == null) showAuthDialog = true else dialogTarget = "已认领"
                        }
                )
            }
        }

        RecordRow(
            symbol = "↔",
            title = "智能关联",
            subtitle = when {
                currentUser == null -> "登录后自动比对失卡与认领进度"
                matchingPosts.isNotEmpty() -> "🎉 发现 ${matchingPosts.size} 张可能属于您的卡片！"
                else -> "系统实时为您比对新发布的拾卡信息"
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("设置与支持", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
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
            allUsers = allUsers,
            onDismiss = { showProfileEditDialog = false },
            onSave = onSaveUser
        )
    }

    dialogTarget?.let { target ->
        when (target) {
            "我的发布", "未认领", "已认领", "我的拾卡" -> {
                val dialogTitle = when (target) {
                    "未认领" -> "未认领卡片"
                    "已认领" -> "已认领卡片"
                    else -> "我的发布记录"
                }
                val myPosts = when (target) {
                    "未认领" -> userPosts.filter { !it.status.contains("已归还") && !it.status.contains("认领") }
                    "已认领" -> userPosts.filter { it.status.contains("已归还") || it.status.contains("认领") }
                    else -> userPosts
                }
                AlertDialog(
                    onDismissRequest = { dialogTarget = null },
                    title = { Text(dialogTitle, color = ScreenInk, fontWeight = FontWeight.Bold) },
                    text = {
                        if (myPosts.isEmpty()) {
                            Text(
                                when (target) {
                                    "未认领" -> "暂无未认领卡片记录。"
                                    "已认领" -> "暂无已认领卡片记录。"
                                    else -> "目前还没有发布记录。您可以在“发布”页面登记拾获的卡片。"
                                },
                                color = ScreenMuted
                            )
                        } else {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                myPosts.forEach { post ->
                                    val docName = post.title.substringBefore(" · ").ifBlank { "证件" }
                                    val statusDisplay = when {
                                        post.status.contains("已归还") || post.status.contains("认领") -> "已归还失主"
                                        post.status == "还在原地" -> "还在原地"
                                        else -> post.status.ifBlank { "由我代为保管" }
                                    }
                                    val statusTint = when {
                                        statusDisplay.contains("归还") || statusDisplay.contains("认领") -> Color(0xFF7C3AED)
                                        statusDisplay == "还在原地" -> SuccessGreen
                                        else -> WarmOrange
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF7F9FB),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clickable { selectedDetailPost = post }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(docName, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                TagPill(statusDisplay, statusTint)
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

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text("点击查看详细信息卡片 ›", color = BrandBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                    val docName = post.title.substringBefore(" · ").ifBlank { "证件" }
                                    val statusDisplay = when {
                                        post.status.contains("已归还") || post.status.contains("认领") -> "已归还失主"
                                        post.status == "还在原地" -> "还在原地"
                                        else -> post.status.ifBlank { "由发布者代为保管" }
                                    }
                                    val statusTint = when {
                                        statusDisplay.contains("归还") || statusDisplay.contains("认领") -> Color(0xFF7C3AED)
                                        statusDisplay == "还在原地" -> SuccessGreen
                                        else -> WarmOrange
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF9F5),
                                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDetailPost = post }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(docName, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                TagPill(statusDisplay, statusTint)
                                            }
                                            Text("卡片姓名：${post.personName} · 号码：${post.documentNumber}", color = ScreenInk, fontSize = 12.sp)
                                            Text("拾获地点：${post.campus} · ${post.location}", color = ScreenMuted, fontSize = 12.sp)
                                            Text("拾获时间：${post.date}", color = ScreenMuted, fontSize = 11.sp)
                                            if (post.latitude != null && post.longitude != null) {
                                                Text("📍 附带经纬度坐标", color = ScreenMuted, fontSize = 11.sp)
                                            }
                                            if (post.imageUri.isNotBlank()) {
                                                Text("📷 已附带现场照片", color = BrandBlue, fontSize = 11.sp)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text("点击查看详细信息卡片 ›", color = WarmOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

    selectedDetailPost?.let { post ->
        val docName = post.title.substringBefore(" · ").ifBlank { "证件" }
        val statusDisplay = when {
            post.status.contains("已归还") || post.status.contains("认领") -> "已归还失主"
            post.status == "还在原地" -> "还在原地"
            else -> post.status.ifBlank { "由发布者代为保管" }
        }
        val statusTint = when {
            statusDisplay.contains("归还") || statusDisplay.contains("认领") -> Color(0xFF7C3AED)
            statusDisplay == "还在原地" -> SuccessGreen
            else -> WarmOrange
        }

        AlertDialog(
            onDismissRequest = { selectedDetailPost = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(docName, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    TagPill(text = statusDisplay, tint = statusTint)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (post.personName.isNotBlank()) {
                                Text("失主姓名：${post.personName}", color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            if (post.documentNumber.isNotBlank()) {
                                Text("证件号码：${post.documentNumber}", color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Text("⌖  ${post.campus} · ${post.location}\n发布时间：${post.date}", color = ScreenMuted, fontSize = 12.sp, lineHeight = 18.sp)

                    if (post.latitude != null && post.longitude != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "📍 物品精确位置坐标",
                                    color = BrandBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "北纬 ${"%.4f".format(post.latitude)}°，东经 ${"%.4f".format(post.longitude)}°",
                                    color = ScreenInk,
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = {
                                        openMapWithCoordinates(
                                            context = context,
                                            latitude = post.latitude,
                                            longitude = post.longitude,
                                            label = "${post.campus} - ${post.location}"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                ) {
                                    Text("🗺️ 跳转地图导航查看位置", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (post.imageUri.isNotBlank()) {
                        val bitmap = remember(post.imageUri) { loadBitmapFromUriOrPath(context, post.imageUri) }
                        if (bitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "📷 物品现场实拍照片",
                                        color = ScreenInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewingPhotoUri = post.imageUri },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "现场照片",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text("点击照片可放大预览", color = ScreenMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (!post.status.contains("已归还")) {
                        Button(
                            onClick = {
                                onUpdatePostStatus(post.id, "已归还失主")
                                selectedDetailPost = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("标记为已归还 / 确认已取回", color = Color.White, fontSize = 13.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                "该证件已成功归还失主并完成归档。",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDetailPost = null }) {
                    Text("返回", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }

    viewingPhotoUri?.let { uri ->
        val bmp = remember(uri) { loadBitmapFromUriOrPath(context, uri) }
        Dialog(onDismissRequest = { viewingPhotoUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("物品现场实拍大图", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ScreenInk)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "放大照片",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Button(
                        onClick = { viewingPhotoUri = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("关闭", color = Color.White)
                    }
                }
            }
        }
    }
}
