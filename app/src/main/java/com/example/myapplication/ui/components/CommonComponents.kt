package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.LostFoundPost
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.domain.validation.ValidationHelper
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.Capsule

@Composable
fun PageShell(
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
fun SearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String) {
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
fun LostFoundCard(post: LostFoundPost, onClick: () -> Unit) {
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
    val maskedName = if (post.personName.isNotBlank()) ValidationHelper.maskPersonName(post.personName) else ""
    val maskedDoc = if (post.documentNumber.isNotBlank()) ValidationHelper.maskDocumentNumber(post.title, post.documentNumber) else ""

    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TagPill(docName, BrandBlue)
            TagPill(statusDisplay, statusTint)
            Spacer(Modifier.weight(1f))
            Text(if (post.isDemo) "演示" else "刚发布", color = ScreenMuted, fontSize = 11.sp)
        }

        if (maskedName.isNotBlank() || maskedDoc.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
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

        Spacer(Modifier.height(8.dp))
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
fun TagPill(text: String, tint: Color) {
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
fun FormTextField(
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
fun DocumentTypeDropdown(
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
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
fun PrimaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandBlue)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyCard(title: String, subtitle: String, action: String, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = ScreenInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = ScreenMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Text(action + "  →", color = BrandBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
fun ProfileCard(
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
fun StatValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = ScreenInk, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(label, color = ScreenMuted, fontSize = 11.sp)
    }
}

@Composable
fun RecordRow(symbol: String, title: String, subtitle: String, count: Int, tint: Color, onClick: () -> Unit) {
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
fun SettingRow(title: String, onClick: () -> Unit) {
    InfoCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = ScreenInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("›", color = ScreenMuted, fontSize = 20.sp)
        }
    }
}

@Composable
fun InfoCard(
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
