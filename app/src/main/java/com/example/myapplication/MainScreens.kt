package com.example.myapplication

import android.net.Uri
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.Capsule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

internal data class LostFoundPost(
    val type: String,
    val status: String,
    val title: String,
    val campus: String,
    val location: String,
    val date: String,
    val note: String,
    val isDemo: Boolean
)

private val ScreenInk = Color(0xFF1E2D3D)
private val ScreenMuted = Color(0xFF718093)
private val BrandBlue = Color(0xFF2878C8)
private val SuccessGreen = Color(0xFF278566)
private val WarmOrange = Color(0xFFDE8958)
private val CardWhite = Color.White

private val demoPosts = listOf(
    LostFoundPost(
        type = "拾卡",
        status = "待认领",
        title = "校园卡 · 尾号 ··3821",
        campus = "江安校区",
        location = "第一教学楼附近",
        date = "10月29日",
        note = "在教学楼附近拾获，卡片信息已隐藏。认领时请自行核对卡面信息。",
        isDemo = true
    ),
    LostFoundPost(
        type = "寻卡",
        status = "寻找中",
        title = "校园卡 · 卡号已隐藏",
        campus = "望江校区",
        location = "图书馆附近",
        date = "10月28日",
        note = "大约在下午遗失，如有线索请通过安全的方式联系失主。",
        isDemo = true
    ),
    LostFoundPost(
        type = "拾卡",
        status = "已交招领处",
        title = "校园卡 · 卡号已隐藏",
        campus = "华西校区",
        location = "食堂入口附近",
        date = "10月27日",
        note = "已交至校区失物招领处。此条为界面演示信息。",
        isDemo = true
    )
)

@Composable
internal fun DiscoverScreen(
    backdrop: LayerBackdrop,
    userPosts: List<LostFoundPost>,
    onStartPublish: () -> Unit
) {
    // Search terms can contain card identifiers, so keep them out of saved state.
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("全部") }
    var selectedCampus by rememberSaveable { mutableStateOf("全部校区") }
    var selectedPost by remember { mutableStateOf<LostFoundPost?>(null) }
    LaunchedEffect(userPosts.size) {
        if (userPosts.isNotEmpty()) {
            selectedFilter = "全部"
            selectedCampus = "全部校区"
        }
    }
    val posts = userPosts + demoPosts
    val filteredPosts = posts.filter { post ->
        (selectedFilter == "全部" || post.type == selectedFilter) &&
            (selectedCampus == "全部校区" || post.campus.startsWith(selectedCampus)) &&
            (searchQuery.isBlank() || listOf(post.title, post.campus, post.location, post.note)
                .any { it.contains(searchQuery.trim(), ignoreCase = true) })
    }

    PageShell(backdrop) {
        PageHeader(
            eyebrow = "SCU · FIND MY CARD",
            subtitle = "让遗失的校园卡，重新找到主人。"
        )

        SearchBox(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "输入卡号尾号、校区或地点"
        )

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("最新信息", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
                    Text("校园卡信息已脱敏展示", fontSize = 12.sp, color = ScreenMuted)
                }
                Text("${filteredPosts.size} 条", fontSize = 12.sp, color = ScreenMuted)
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("全部校区", "江安", "望江", "华西").forEach { campus ->
                    SelectChip(
                        text = campus,
                        selected = selectedCampus == campus,
                        onClick = { selectedCampus = campus }
                    )
                }
            }
        }

        if (filteredPosts.isEmpty()) {
            EmptyCard(
                title = "没有找到相关信息",
                subtitle = "试试更短的关键词，或登记你捡到的卡片。",
                action = "登记拾获信息",
                onClick = onStartPublish
            )
        } else {
            filteredPosts.forEach { post ->
                LostFoundCard(post = post, onClick = { selectedPost = post })
            }
        }
        Text(
            text = "演示列表仅用于展示页面布局，正式内容需由用户发布。",
            color = ScreenMuted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }

    selectedPost?.let { post ->
        AlertDialog(
            onDismissRequest = { selectedPost = null },
            title = { Text(post.title, color = ScreenInk, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("${post.type}信息 · ${post.status}", color = BrandBlue, fontWeight = FontWeight.SemiBold)
                    Text("${post.campus} · ${post.location}\n${post.date}发布", color = ScreenMuted)
                    Text(post.note, color = ScreenInk)
                    Text("公开页面不会显示完整校园卡号。", color = ScreenMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPost = null }) { Text("知道了", color = BrandBlue) }
            },
            containerColor = Color.White
        )
    }
}

@Composable
internal fun PublishScreen(
    backdrop: LayerBackdrop,
    onSubmit: (LostFoundPost) -> Unit
) {
    // Full card identifiers stay in transient UI state and are never stored in the post.
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cardInfo by remember { mutableStateOf("") }
    var campus by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var foundDate by rememberSaveable { mutableStateOf("今天") }
    var custodyStatus by rememberSaveable { mutableStateOf("由我暂时保管") }
    var error by rememberSaveable { mutableStateOf("") }
    var ocrMessage by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(false) }
    var isScannerOpen by remember { mutableStateOf(false) }

    val recognizeImage: (Uri, File?) -> Unit = { imageUri, temporaryFile ->
        coroutineScope.launch {
            isRecognizing = true
            ocrMessage = "正在定位卡片并识别文字…"
            try {
                val result = CardOcr.scan(context, imageUri)
                when (result.type) {
                    RecognizedCardType.CAMPUS_CARD -> {
                        val suffix = result.cardNumberSuffix
                        if (suffix != null) {
                            cardInfo = "尾号 $suffix"
                            error = ""
                            ocrMessage = "识别为校园卡（${result.matchedKeyword ?: "卡面特征"}），已填入尾号 $suffix，请核对。"
                        } else {
                            cardInfo = "校园卡"
                            error = ""
                            ocrMessage = "识别为校园卡（${result.matchedKeyword ?: "卡面特征"}），没有找到清晰卡号，请补充外观特征。"
                        }
                    }
                    RecognizedCardType.IDENTITY_CARD,
                    RecognizedCardType.HOME_RETURN_PERMIT -> {
                        cardInfo = ""
                        ocrMessage = "识别为${result.type.title}；此登记只接受校园卡，未回填证件号码。"
                    }
                    RecognizedCardType.OTHER -> {
                        cardInfo = ""
                        ocrMessage = if (result.lines.isEmpty()) {
                            "没有识别到文字。请让卡片正面朝上、文字清晰并完整进入取景框，避免反光后重拍。"
                        } else {
                            "已识别到卡面文字，但类型不明确；请手动确认这是校园卡后填写。"
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                val reason = exception.localizedMessage?.take(100)
                ocrMessage = if (reason.isNullOrBlank()) {
                    "卡片定位或识别失败，请调整角度后重试。"
                } else {
                    "识别失败：$reason"
                }
            } finally {
                temporaryFile?.delete()
                isRecognizing = false
            }
        }
    }

    PageShell(backdrop) {
        Text("登记拾获信息", fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
        FormTextField(
            label = "校园卡尾号或外观特征",
            value = cardInfo,
            onValueChange = { cardInfo = it; error = "" },
            placeholder = "例如：尾号 3821 / 蓝色卡套；不要填写完整卡号"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OcrActionButton(
                title = if (isRecognizing) "正在识别…" else "扫描卡片",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecognizing
            ) {
                isScannerOpen = true
            }
        }
        Text(
            "对齐卡片后拍摄；系统会先矫正卡面，再按文字内容与位置判断类型。仅校园卡可回填尾号。",
            color = ScreenMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        if (ocrMessage.isNotBlank()) {
            val isOcrError = ocrMessage.startsWith("识别失败") ||
                ocrMessage.startsWith("无法") ||
                ocrMessage.startsWith("未找到")
            Text(
                ocrMessage,
                color = if (isOcrError) Color(0xFFB5473C) else BrandBlue,
                fontSize = 12.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("拾获校区", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("江安校区", "望江校区", "华西校区", "华西第二校区").forEach { campusName ->
                    SelectChip(campusName, selected = campus == campusName) {
                        campus = campusName
                        error = ""
                    }
                }
            }
        }
        FormTextField(
            label = "拾获地点",
            value = location,
            onValueChange = { location = it; error = "" },
            placeholder = "例如：第一教学楼附近"
        )
        FormTextField(
            label = "拾获日期",
            value = foundDate,
            onValueChange = { foundDate = it; error = "" },
            placeholder = "例如：今天"
        )
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("卡片当前状态", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("由我暂时保管", "已交至招领处", "已归还失主").forEach { status ->
                    SelectChip(status, selected = custodyStatus == status) { custodyStatus = status }
                }
            }
        }
        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFB5473C), fontSize = 13.sp)
        }
        PrimaryActionButton("发布拾卡信息") {
            if (cardInfo.isBlank() || campus.isBlank() || location.isBlank() || foundDate.isBlank()) {
                error = "请填写卡片特征、校区、地点和日期。"
            } else {
                val digits = cardInfo.filter { it.isDigit() }
                val safeTitle = when {
                    digits.length >= 4 -> "校园卡 · 尾号 ··${digits.takeLast(4)}"
                    digits.isEmpty() -> "校园卡 · ${cardInfo.trim().take(16)}"
                    else -> "校园卡 · 卡号已隐藏"
                }
                val safeStatusNote = when (custodyStatus) {
                    "已交至招领处" -> "卡片已交至失物招领处。"
                    "已归还失主" -> "卡片已归还失主。"
                    else -> "卡片目前由拾获者暂时保管。"
                }
                onSubmit(
                    LostFoundPost(
                        type = "拾卡",
                        status = custodyStatus,
                        title = safeTitle,
                        campus = campus,
                        location = location,
                        date = foundDate,
                        note = safeStatusNote,
                        isDemo = false
                    )
                )
            }
        }
    }

    if (isScannerOpen) {
        CardScannerDialog(
            onDismiss = { isScannerOpen = false },
            onImageCaptured = { uri, file ->
                isScannerOpen = false
                recognizeImage(uri, file)
            },
            onImageSelected = { uri ->
                isScannerOpen = false
                recognizeImage(uri, null)
            }
        )
    }
}

@Composable
internal fun ProfileScreen(backdrop: LayerBackdrop, userPosts: List<LostFoundPost>) {
    var dialogTarget by remember { mutableStateOf<String?>(null) }
    val openPosts = userPosts.filter { it.status !in setOf("已归还", "已归还失主", "已解决") }
    val completedPosts = userPosts.size - openPosts.size

    PageShell(backdrop) {
        PageHeader(
            eyebrow = "MY CAMPUS",
            subtitle = "个人信息与拾卡记录"
        )
        ProfileCard(onClick = { dialogTarget = "个人资料" })

        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatValue("我的发布", userPosts.size, Modifier.weight(1f))
                StatValue("处理中", openPosts.size, Modifier.weight(1f))
                StatValue("已完成", completedPosts, Modifier.weight(1f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("我的记录", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
            RecordRow(
                symbol = "✓",
                title = "我的拾卡",
                subtitle = "查看我登记的拾获信息",
                count = userPosts.count { it.type == "拾卡" },
                tint = SuccessGreen,
                onClick = { dialogTarget = "我的拾卡" }
            )
            RecordRow(
                symbol = "↔",
                title = "我的认领",
                subtitle = "查看认领和归还进度",
                count = 0,
                tint = WarmOrange,
                onClick = { dialogTarget = "我的认领" }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("其他", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
            listOf("个人资料", "隐私与安全", "关于川大寻卡").forEach { item ->
                SettingRow(item) { dialogTarget = item }
            }
        }
    }

    dialogTarget?.let { target ->
        val matchingPosts = when (target) {
            "我的拾卡" -> userPosts.filter { it.type == "拾卡" }
            else -> emptyList()
        }
        val isRecordDialog = target.startsWith("我的")
        AlertDialog(
            onDismissRequest = { dialogTarget = null },
            title = { Text(target, color = ScreenInk, fontWeight = FontWeight.Bold) },
            text = {
                if (isRecordDialog && matchingPosts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        matchingPosts.forEach { post ->
                            Text(post.title, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                            Text("${post.campus} · ${post.location} · ${post.status}", color = ScreenMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        text = when {
                            target == "我的认领" -> "目前还没有认领记录。后续可从匹配信息的详情页发起认领。"
                            isRecordDialog -> "目前还没有拾卡记录。发布拾卡信息后，会显示在这里。"
                            target == "隐私与安全" -> "发布信息会隐藏完整校园卡号。请勿公开填写身份证号或其他敏感资料。"
                            target == "关于川大寻卡" -> "川大寻卡帮助校园卡失主与拾获者更快建立联系。当前界面为本地交互原型。"
                            else -> "个人资料编辑入口已预留。"
                        },
                        color = ScreenMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogTarget = null }) { Text("关闭", color = BrandBlue) }
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
    Box(Modifier.fillMaxSize()) {
        WhitePageBackground(Modifier.matchParentSize().layerBackdrop(backdrop))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 112.dp),
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
private fun PageHeader(eyebrow: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = BrandBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text(subtitle, color = ScreenMuted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
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
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TagPill(post.type + "信息", tint)
            TagPill(post.status, if (post.status == "待认领") WarmOrange else tint.copy(alpha = .76f))
            Spacer(Modifier.weight(1f))
            Text(if (post.isDemo) "演示" else "刚发布", color = ScreenMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(post.title, color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    maxLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE8EDF2), RoundedCornerShape(14.dp))
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = maxLines == 1,
                maxLines = maxLines,
                textStyle = TextStyle(color = ScreenInk, fontSize = 14.sp, lineHeight = 20.sp),
                cursorBrush = SolidColor(BrandBlue),
                modifier = Modifier.fillMaxWidth().heightIn(min = if (maxLines == 1) 48.dp else 92.dp).padding(13.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = ScreenMuted, fontSize = 13.sp, lineHeight = 19.sp)
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun OcrActionButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = if (enabled) Color(0xFFEAF3FB) else Color(0xFFF2F4F6),
        modifier = modifier
            .heightIn(min = 46.dp)
            .border(1.dp, Color(0xFFD8E8F5), RoundedCornerShape(13.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = if (enabled) BrandBlue else ScreenMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
private fun ProfileCard(onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE5E0FF)),
                contentAlignment = Alignment.Center
            ) {
                Text("我", color = Color(0xFF7569C5), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("校园寻卡用户", color = ScreenInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("完善个人资料，方便管理寻卡进度", color = ScreenMuted, fontSize = 12.sp)
            }
            Text("›", color = ScreenMuted, fontSize = 23.sp)
        }
    }
}

@Composable
private fun StatValue(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.toString(), color = ScreenInk, fontSize = 21.sp, fontWeight = FontWeight.Bold)
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
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            content = content
        )
    }
}
