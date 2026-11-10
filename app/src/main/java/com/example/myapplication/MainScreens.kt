package com.example.myapplication

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DocumentTypeOption(
    val name: String,
    val identifierLabel: String,
    val identifierPlaceholder: String,
    val personNameLabel: String,
    val personNamePlaceholder: String
)

private val documentTypeOptions = listOf(
    DocumentTypeOption("校园卡", "学号", "请输入13位学号", "姓名", "请输入卡面姓名"),
    DocumentTypeOption("居民身份证", "身份证号码", "请输入18位身份证号码", "证件姓名", "请输入证件上的姓名"),
    DocumentTypeOption("港澳居民来往内地通行证", "通行证号码", "请输入完整通行证号码", "证件姓名", "请输入证件上的姓名"),
    DocumentTypeOption("护照", "护照号码", "请输入完整护照号码", "护照姓名", "请输入证件上的姓名"),
    DocumentTypeOption("其他证件", "证件号码", "请输入完整证件号码", "证件姓名", "请输入证件上的姓名")
)

private val ScreenInk = Color(0xFF1E2D3D)
private val ScreenMuted = Color(0xFF718093)
private val BrandBlue = Color(0xFF2878C8)
private val SuccessGreen = Color(0xFF278566)
private val WarmOrange = Color(0xFFDE8958)
private val CardWhite = Color.White

private fun getPostDisplayTitle(post: LostFoundPost): String {
    val docName = post.title.substringBefore(" · ").ifBlank { "证件" }
    val status = when {
        post.status.contains("已归还") -> "已归还失主"
        post.status == "还在原地" -> "还在原地"
        post.status == "由我暂时保管" || post.status == "由发布者代为保管" || post.status == "待认领" -> "由发布者代为保管"
        post.status == "寻找中" -> "寻找中"
        else -> post.status
    }
    return "$docName · $status"
}

private val demoPosts = listOf(
    LostFoundPost(
        type = "拾卡",
        status = "还在原地",
        title = "校园卡 · 还在原地",
        campus = "江安校区",
        location = "第一教学楼附近",
        date = "10月29日",
        note = "",
        isDemo = true,
        personName = "张三丰",
        documentNumber = "2023141440001"
    ),
    LostFoundPost(
        type = "拾卡",
        status = "由发布者代为保管",
        title = "居民身份证 · 由发布者代为保管",
        campus = "望江校区",
        location = "图书馆附近",
        date = "10月28日",
        note = "",
        isDemo = true,
        personName = "李四",
        documentNumber = "510107200305011234"
    ),
    LostFoundPost(
        type = "拾卡",
        status = "还在原地",
        title = "学生证 · 还在原地",
        campus = "华西校区",
        location = "食堂入口附近",
        date = "10月27日",
        note = "",
        isDemo = true,
        personName = "诸葛孔明",
        documentNumber = "EE12345678"
    )
)

@Composable
internal fun DiscoverScreen(
    backdrop: LayerBackdrop,
    userPosts: List<LostFoundPost>,
    currentUser: UserProfile? = null,
    onUpdatePostStatus: (String, String) -> Unit = { _, _ -> },
    onStartPublish: () -> Unit
) {
    // Search terms can contain card identifiers, so keep them out of saved state.
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("全部校区") }
    var selectedPost by remember { mutableStateOf<LostFoundPost?>(null) }

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
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "✨ 系统识别该卡片证件号与您的实名档案完全匹配！",
                                    color = Color(0xFFEA580C),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                        Text("完整号码和姓名仅在实名完全匹配或“我的拾卡”中可管理。", color = ScreenMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPost = null }) { Text("关闭", color = BrandBlue) }
            },
            containerColor = Color.White
        )
    }
}

private fun formatCurrentDateTime(): String {
    val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

@Composable
internal fun PublishScreen(
    backdrop: LayerBackdrop,
    currentUser: UserProfile?,
    allUsers: List<UserProfile>,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPassword: (String, String, String, (EduroamResult) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onSaveUser: (UserProfile) -> Unit,
    onSubmit: (LostFoundPost) -> Unit
) {
    // Full document details stay on the in-memory post and are omitted from public listings.
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    var documentIdentifier by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var selectedDocumentName by rememberSaveable { mutableStateOf("校园卡") }
    var campus by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var foundDate by remember { mutableStateOf(formatCurrentDateTime()) }
    var custodyStatus by rememberSaveable { mutableStateOf("由发布者代为保管") }
    var error by rememberSaveable { mutableStateOf("") }
    val selectedDocument = documentTypeOptions.first { it.name == selectedDocumentName }

    var permissionLauncherRef: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null

    val fetchLocationAction: (Boolean) -> Unit = { requestPermission ->
        if (LocationHelper.hasLocationPermission(context)) {
            coroutineScope.launch {
                isLocating = true
                try {
                    val result = LocationHelper.fetchCurrentLocation(context)
                    if (result != null) {
                        location = result.locationDescription
                        error = ""
                        if (campus.isBlank() && result.campus != null) {
                            campus = result.campus
                        }
                    }
                } finally {
                    isLocating = false
                }
            }
        } else if (requestPermission) {
            permissionLauncherRef?.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            fetchLocationAction(false)
        }
    }
    permissionLauncherRef = permissionLauncher

    LaunchedEffect(Unit) {
        foundDate = formatCurrentDateTime()
        fetchLocationAction(true)
    }

    PageShell(backdrop) {
        Text("登记拾获信息", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = ScreenInk)

        if (currentUser == null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF7ED),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAuthDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("您当前处于未登录状态", color = Color(0xFFC2410C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("登记拾获信息需要学号实名认证，点击立即登录 / 注册", color = Color(0xFFEA580C), fontSize = 11.sp)
                    }
                    Text("去登录 ›", color = Color(0xFFC2410C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        InfoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("证件类型", fontSize = 14.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                DocumentTypeDropdown(
                    selected = selectedDocumentName,
                    options = documentTypeOptions.map { it.name },
                    onSelected = { name ->
                        selectedDocumentName = name
                        documentIdentifier = ""
                        personName = ""
                        error = ""
                    }
                )
            }
        }

        InfoCard(Modifier.fillMaxWidth()) {
            FormTextField(
                label = selectedDocument.identifierLabel,
                value = documentIdentifier,
                onValueChange = { documentIdentifier = it; error = "" },
                placeholder = selectedDocument.identifierPlaceholder
            )
            FormTextField(
                label = selectedDocument.personNameLabel,
                value = personName,
                onValueChange = { personName = it; error = "" },
                placeholder = selectedDocument.personNamePlaceholder
            )
        }

        InfoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("拾获校区", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("江安校区", "望江校区", "华西校区").forEach { campusName ->
                        SelectChip(campusName, selected = campus == campusName) {
                            campus = campusName
                            error = ""
                        }
                    }
                }
            }
            FormTextField(
                label = "具体地点",
                value = location,
                onValueChange = { location = it; error = "" },
                placeholder = if (isLocating) "正在定位当前位置…" else "例如：第一教学楼附近",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEAF3FB))
                            .clickable(enabled = !isLocating) {
                                fetchLocationAction(true)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⌖",
                            color = BrandBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLocating) "定位中…" else "自动定位",
                            color = BrandBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
            FormTextField(
                label = "拾获时间",
                value = foundDate,
                onValueChange = { foundDate = it; error = "" },
                placeholder = "例如：2026年11月10日 18:30"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("证件当前状态", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("由发布者代为保管", "还在原地").forEach { status ->
                    SelectChip(status, selected = custodyStatus == status) { custodyStatus = status }
                }
            }
        }
        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFB5473C), fontSize = 13.sp)
        }
        PrimaryActionButton("发布拾获信息") {
            if (currentUser == null) {
                showAuthDialog = true
                return@PrimaryActionButton
            }
            if (documentIdentifier.isBlank() || personName.isBlank() || campus.isBlank() || location.isBlank() || foundDate.isBlank()) {
                error = "请填写完整号码、姓名、校区、地点和拾获时间。"
            } else {
                if (selectedDocumentName == "校园卡") {
                    val idErr = ValidationHelper.validateStudentId(documentIdentifier)
                    if (idErr != null) {
                        error = idErr
                        return@PrimaryActionButton
                    }
                } else if (selectedDocumentName == "居民身份证") {
                    val idErr = ValidationHelper.validateIdCard(documentIdentifier)
                    if (idErr != null) {
                        error = idErr
                        return@PrimaryActionButton
                    }
                }
                val nameErr = ValidationHelper.validateName(personName)
                if (nameErr != null) {
                    error = nameErr
                    return@PrimaryActionButton
                }

                val statusDisplay = if (custodyStatus == "还在原地") "还在原地" else "由发布者代为保管"
                val safeTitle = "$selectedDocumentName · $statusDisplay"
                val cleanDocNumber = if (selectedDocumentName == "居民身份证") {
                    documentIdentifier.replace(" ", "").trim().uppercase()
                } else if (selectedDocumentName == "校园卡") {
                    documentIdentifier.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
                } else {
                    documentIdentifier.trim()
                }
                onSubmit(
                    LostFoundPost(
                        type = "拾卡",
                        status = statusDisplay,
                        title = safeTitle,
                        campus = campus,
                        location = location,
                        date = foundDate.trim(),
                        note = "",
                        isDemo = false,
                        personName = personName.trim(),
                        documentNumber = cleanDocNumber
                    )
                )
                documentIdentifier = ""
                personName = ""
                location = ""
                error = ""
                foundDate = formatCurrentDateTime()
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
}

@Composable
internal fun ProfileScreen(
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
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)),
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
                                "隐私与安全" -> "1. 校园网密码仅在实名认证时单次提交至官方 eduroam 验证接口，应用本地绝不保存密码。\n2. 完整证件号码与姓名存储于本地数据库用于匹配，公开信息广场脱敏展示。\n3. 请仅在您的个人可信设备上登录使用。"
                                "关于川大寻卡" -> "川大寻卡致力于帮助四川大学失主与拾获者建立快捷、可信的联系。\n集成 eduroam 官方学号认证与智能失物比对通知引擎。\n当前版本为本地交互与认证原型系统。"
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

@Composable
internal fun RegisterScreen(
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
            Text("实名注册档案", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
            Text(
                "通过 eduroam 验证学号真实性，卡片拾获时系统将自动匹配提醒",
                fontSize = 12.sp,
                color = ScreenMuted,
                lineHeight = 17.sp
            )
        }

        InfoCard(Modifier.fillMaxWidth()) {
            FormTextField(
                label = "真实姓名 * (必填)",
                value = name,
                onValueChange = { name = it; errorMessage = "" },
                placeholder = "例如：张三"
            )

            FormTextField(
                label = "四川大学学号 * (必填)",
                value = studentId,
                onValueChange = { studentId = it; errorMessage = "" },
                placeholder = "例如：2022141440000"
            )

            FormTextField(
                label = "校园网/统一身份密码 * (仅单次官方核验)",
                value = eduroamPassword,
                onValueChange = { eduroamPassword = it; errorMessage = "" },
                placeholder = "输入四川大学校园网密码",
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
                label = "设置应用登录密码 (选填)",
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
                placeholder = "选填，用于拾卡自动精准比对（若填须完整18位）"
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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F7FA),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛡️", fontSize = 15.sp)
                Text(
                    "校园网密码仅用于单次向 eduroam 验证学号所有权。若未自定义应用密码，将默认以校园网密码作为初始应用登录密码。",
                    fontSize = 11.sp,
                    color = ScreenMuted,
                    lineHeight = 16.sp
                )
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
                    Text("正在连接 eduroam 官方验证服务器…", color = ScreenMuted, fontSize = 12.sp)
                }
            }
        }

        PrimaryActionButton(
            text = if (isLoading) "正在验证中…" else "完成 eduroam 验证并注册",
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
                    errorMessage = "请填写校园网/eduroam密码以完成身份验证"
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

@Composable
internal fun AuthDialog(
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

@Composable
internal fun ProfileEditDialog(
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var phone by remember { mutableStateOf(currentUser.phone) }
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
                        Text("核心实名凭据已认证绑定", fontSize = 12.sp, color = ScreenMuted)
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
                            if (currentUser.idCard.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val maskedId = if (currentUser.idCard.length == 18) {
                                        currentUser.idCard.replaceRange(6, 14, "********")
                                    } else {
                                        currentUser.idCard
                                    }
                                    Text(maskedId, color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("🔒", fontSize = 11.sp)
                                }
                            } else {
                                Text("未绑定 (选填)", color = ScreenMuted, fontSize = 13.sp)
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
                                "实名信息已唯一绑定，不可自行修改",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScreenInk
                            )
                            Text(
                                "如姓名、身份证需变更，须联系管理员提交证明",
                                fontSize = 11.sp,
                                color = BrandBlue
                            )
                        }
                        Text("申请修改 ›", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FormTextField(
                    label = "联系手机 (选填)",
                    value = phone,
                    onValueChange = { phone = it; errorMessage = "" },
                    placeholder = "方便拾获同学快速联系"
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
                            val phoneErr = ValidationHelper.validatePhone(phone, isOptional = true)
                            if (phoneErr != null) {
                                errorMessage = phoneErr
                                return@Button
                            }
                            val cleanPhone = phone.replace(" ", "").trim()
                            val updated = currentUser.copy(
                                phone = cleanPhone,
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

@Composable
private fun PageShell(
    backdrop: LayerBackdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5F7))
            .layerBackdrop(backdrop)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
            content = content
        )
    }
}

@Composable
private fun WhitePageBackground(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color.White)
    }
}

@Composable
private fun PageHeader(eyebrow: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = BrandBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE8EDF2), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text("⌕", color = BrandBlue, fontSize = 23.sp, lineHeight = 24.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = ScreenInk, fontSize = 14.sp),
                cursorBrush = SolidColor(BrandBlue),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = ScreenMuted, fontSize = 14.sp)
                        innerTextField()
                    }
                }
            )
            if (value.isNotEmpty()) {
                Text("×", color = ScreenMuted, fontSize = 20.sp, modifier = Modifier.clickable { onValueChange("") })
            }
        }
    }
}

@Composable
private fun LostFoundCard(post: LostFoundPost, onClick: () -> Unit) {
    val tint = if (post.type == "拾卡") SuccessGreen else BrandBlue
    val maskedName = if (post.personName.isNotBlank()) ValidationHelper.maskPersonName(post.personName) else ""
    val maskedDoc = if (post.documentNumber.isNotBlank()) ValidationHelper.maskDocumentNumber(post.title, post.documentNumber) else ""
    val displayTitle = getPostDisplayTitle(post)

    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TagPill(post.type + "信息", tint)
            TagPill(post.status, if (post.status == "待认领" || post.status == "由发布者代为保管") WarmOrange else tint.copy(alpha = .76f))
            Spacer(Modifier.weight(1f))
            Text(if (post.isDemo) "演示" else "刚发布", color = ScreenMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(displayTitle, color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (maskedName.isNotBlank() || maskedDoc.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (maskedName.isNotBlank()) {
                    Text("失主：$maskedName", color = ScreenInk, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                if (maskedDoc.isNotBlank()) {
                    Text("证件号：$maskedDoc", color = ScreenMuted, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        Text("⌖  ${post.campus} · ${post.location}", color = ScreenMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${post.date}发布", color = ScreenMuted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("查看详情  ›", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TagPill(text: String, tint: Color) {
    Text(
        text = text,
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(Capsule())
            .background(tint.copy(alpha = .10f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

@Composable
private fun PublishOptionCard(
    symbol: String,
    tint: Color,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, color = tint, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ScreenMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text(buttonLabel + "   →", color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE8EDF2), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (maxLines == 1) 48.dp else 92.dp)
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = if (maxLines == 1) Alignment.CenterVertically else Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = maxLines == 1,
                        maxLines = maxLines,
                        visualTransformation = visualTransformation,
                        textStyle = TextStyle(color = ScreenInk, fontSize = 14.sp, lineHeight = 20.sp),
                        cursorBrush = SolidColor(BrandBlue),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty()) Text(placeholder, color = ScreenMuted, fontSize = 13.sp, lineHeight = 19.sp)
                                innerTextField()
                            }
                        }
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
    }
}

@Composable
private fun DocumentTypeDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF8FAFC),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE1E8EF), RoundedCornerShape(14.dp))
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, color = ScreenInk, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(if (expanded) "⌃" else "⌄", color = BrandBlue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = ScreenInk, fontSize = 14.sp) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val chipColor = if (selected) BrandBlue else Color.White.copy(alpha = .88f)
    Text(
        text = text,
        color = if (selected) Color.White else ScreenMuted,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(Capsule())
            .background(chipColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}

@Composable
private fun PrimaryActionButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlue)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp)
    )
}

@Composable
private fun EmptyCard(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = ScreenMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Text(action + "  →", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
private fun ProfileCard(
    currentUser: UserProfile?,
    onClick: () -> Unit
) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (currentUser != null) Color(0xFFE5E0FF) else Color(0xFFE9ECEF)),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser != null) {
                    Text(
                        currentUser.name.take(1),
                        color = Color(0xFF7569C5),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("👤", fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${currentUser.name} 同学", color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        TagPill("已认证", SuccessGreen)
                    }
                    Text(
                        "${currentUser.campus} · 学号 ${currentUser.studentId}",
                        color = ScreenMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Text("未登录", color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("点击登录 / 注册", color = BrandBlue, fontSize = 12.sp)
                }
            }
            Text("›", color = ScreenMuted, fontSize = 23.sp)
        }
    }
}

@Composable
private fun StatValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = ScreenInk, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ScreenMuted, fontSize = 11.sp)
    }
}

@Composable
private fun RecordRow(symbol: String, title: String, subtitle: String, count: Int, tint: Color, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = ScreenInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = ScreenMuted, fontSize = 11.sp)
            }
            Text(count.toString(), color = ScreenMuted, fontSize = 13.sp)
            Text("›", color = ScreenMuted, fontSize = 20.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = ScreenInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("›", color = ScreenMuted, fontSize = 20.sp)
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.border(1.dp, Color(0xFFE8EDF2), RoundedCornerShape(19.dp)),
        shape = RoundedCornerShape(19.dp),
        color = CardWhite,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            content = content
        )
    }
}
