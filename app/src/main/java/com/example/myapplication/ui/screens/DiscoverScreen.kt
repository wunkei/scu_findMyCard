package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.myapplication.domain.matching.MatchNotificationHelper
import com.example.myapplication.domain.validation.ValidationHelper
import com.example.myapplication.ui.components.*
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun DiscoverScreen(
    backdrop: LayerBackdrop,
    userPosts: List<LostFoundPost>,
    currentUser: UserProfile? = null,
    onUpdatePostStatus: (String, String) -> Unit = { _, _ -> },
    onStartPublish: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("全部校区") }
    var selectedPost by remember { mutableStateOf<LostFoundPost?>(null) }
    var viewingPhotoUri by remember { mutableStateOf<String?>(null) }

    val posts = userPosts + demoPosts
    val isReturnedTab = selectedCategory == "已归还"

    val filteredPosts = posts.filter { post ->
        val statusMatches = if (isReturnedTab) {
            post.status.contains("已归还")
        } else {
            !post.status.contains("已归还") &&
                    (selectedCategory == "全部校区" || post.campus.startsWith(selectedCategory))
        }

        val searchMatches = if (searchQuery.isBlank()) {
            true
        } else {
            val q = searchQuery.trim()
            val qDoc = q.replace(" ", "").uppercase()
            val postDoc = post.documentNumber.replace(" ", "").trim().uppercase()
            val qName = q.replace(" ", "")
            val postName = post.personName.replace(" ", "").trim()

            val nameMatch = postName.isNotBlank() && (postName.equals(qName, ignoreCase = true) || postName.contains(qName, ignoreCase = true))
            val docMatch = postDoc.isNotBlank() && postDoc == qDoc

            nameMatch || docMatch
        }

        statusMatches && searchMatches
    }

    PageShell(backdrop) {
        SearchBox(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "输入失主姓名或完整证件号搜索"
        )

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isReturnedTab) "已归还档案" else "信息广场",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScreenInk
                    )
                }
                Text("${filteredPosts.size} 条", fontSize = 12.sp, color = ScreenMuted)
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("全部校区", "江安", "望江", "华西", "已归还").forEach { category ->
                    SelectChip(
                        text = category,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        if (filteredPosts.isEmpty()) {
            if (isReturnedTab) {
                EmptyCard(
                    title = "暂无已归还卡片",
                    subtitle = "失主确认取回卡片后，相关记录将在此归档展示。",
                    action = "查看待认领信息",
                    onClick = { selectedCategory = "全部校区" }
                )
            } else {
                EmptyCard(
                    title = "没有找到相关信息",
                    subtitle = "试试输入完整的失主姓名或完整的证件号码搜索。",
                    action = "登记拾获信息",
                    onClick = onStartPublish
                )
            }
        } else {
            filteredPosts.forEach { post ->
                LostFoundCard(post = post, onClick = { selectedPost = post })
            }
        }
        Text(
            text = if (isReturnedTab) "此处仅展示已成功归还失主的卡片归档记录。" else "演示列表仅用于展示页面布局，正式内容需由用户发布。",
            color = ScreenMuted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }

    selectedPost?.let { post ->
        AlertDialog(
            onDismissRequest = { selectedPost = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(getPostDisplayTitle(post), color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TagPill(
                        text = post.status,
                        tint = if (post.status.contains("已归还")) SuccessGreen else BrandBlue
                    )
                }
            },
            text = {
                val maskedName = if (post.personName.isNotBlank()) ValidationHelper.maskPersonName(post.personName) else ""
                val maskedDoc = if (post.documentNumber.isNotBlank()) ValidationHelper.maskDocumentNumber(post.title, post.documentNumber) else ""
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "${post.type}信息 · ${post.status}",
                        color = if (post.status.contains("已归还")) SuccessGreen else BrandBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (maskedName.isNotBlank() || maskedDoc.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (maskedName.isNotBlank()) {
                                    Text("失主：$maskedName", color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                if (maskedDoc.isNotBlank()) {
                                    Text("证件号：$maskedDoc", color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("（依据隐私合规政策，大厅公开信息仅脱敏展示）", color = ScreenMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    Text("⌖  ${post.campus} · ${post.location}\n${post.date}发布", color = ScreenMuted)

                    if (post.status.contains("已归还")) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                "该证件已成功归还失主并完成归档，已从待认领广场移出。",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (currentUser != null && MatchNotificationHelper.isPostMatchingUser(post, currentUser)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "✨ 系统识别该卡片证件号与您的实名档案完全匹配！",
                                    color = Color(0xFFEA580C),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (post.latitude != null && post.longitude != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "📍 物品精确位置坐标",
                                                color = BrandBlue,
                                                fontSize = 11.sp,
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
                                                Text("🗺️ 跳转地图导航查看位置", color = Color.White, fontSize = 11.sp)
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
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    "📷 物品当前位置现场照片",
                                                    color = ScreenInk,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
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
                                                Text("点击可放大查看现场照片", color = ScreenMuted, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        onUpdatePostStatus(post.id, "已归还失主")
                                        selectedPost = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("确认这是我的卡 · 标记已取回", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text("完整号码、精准经纬度导航与现场照片仅在实名完全匹配后可见。", color = ScreenMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPost = null }) { Text("关闭", color = BrandBlue) }
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
                    Text("物品现场照片", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ScreenInk)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "放大照片",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
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
