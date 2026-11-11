package com.example.myapplication.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.myapplication.data.model.LostFoundPost
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.domain.auth.EduroamResult
import com.example.myapplication.domain.validation.ValidationHelper
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.dialogs.AuthDialog
import com.example.myapplication.util.LocationHelper
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PublishScreen(
    backdrop: LayerBackdrop,
    currentUser: UserProfile?,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPassword: (String, String, String, (EduroamResult) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onSubmit: (LostFoundPost) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    var documentIdentifier by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var selectedDocumentName by rememberSaveable { mutableStateOf("校园卡") }
    var campus by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var currentLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    var capturedImageUriString by rememberSaveable { mutableStateOf("") }
    var isPendingPublishAfterPhoto by remember { mutableStateOf(false) }

    var foundDate by remember { mutableStateOf(formatCurrentDateTime()) }
    var custodyStatus by rememberSaveable { mutableStateOf("还在原地") }
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
                        currentLatitude = result.latitude
                        currentLongitude = result.longitude
                        error = ""
                        if (campus.isBlank() && result.campus != null) {
                            campus = result.campus
                        }
                    } else if (currentLatitude == null) {
                        val coords = LocationHelper.getCampusDefaultCoords(campus.ifBlank { "江安校区" })
                        currentLatitude = coords.first
                        currentLongitude = coords.second
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

    val doSubmitPost: (String) -> Unit = { imagePath ->
        val finalLat = currentLatitude ?: LocationHelper.getCampusDefaultCoords(campus.ifBlank { "江安校区" }).first
        val finalLng = currentLongitude ?: LocationHelper.getCampusDefaultCoords(campus.ifBlank { "江安校区" }).second

        val statusDisplay = if (custodyStatus == "还在原地") "还在原地" else "由发布者代为保管"
        val safeTitle = "$selectedDocumentName · $statusDisplay"
        val cleanDocNumber = when (selectedDocumentName) {
            "居民身份证" -> documentIdentifier.replace(" ", "").trim().uppercase()
            "校园卡" -> documentIdentifier.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
            else -> documentIdentifier.trim()
        }
        val finalLocation = if (location.isNotBlank()) {
            location.trim()
        } else if (custodyStatus == "由发布者代为保管") {
            "由发布者代为保管"
        } else {
            "具体地点未填写"
        }

        onSubmit(
            LostFoundPost(
                type = "拾卡",
                status = statusDisplay,
                title = safeTitle,
                campus = campus.ifBlank { "江安校区" },
                location = finalLocation,
                date = foundDate.trim().ifBlank { formatCurrentDateTime() },
                note = "",
                isDemo = false,
                personName = personName.trim(),
                documentNumber = cleanDocNumber,
                latitude = finalLat,
                longitude = finalLng,
                imageUri = imagePath
            )
        )
        documentIdentifier = ""
        personName = ""
        location = ""
        error = ""
        capturedImageUriString = ""
        capturedImageFile = null
        foundDate = formatCurrentDateTime()
        custodyStatus = "还在原地"
        Toast.makeText(context, "发布成功！已上传经纬度位置${if (imagePath.isNotBlank()) "与现场照片" else ""}", Toast.LENGTH_SHORT).show()
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val path = capturedImageFile?.absolutePath.orEmpty()
            capturedImageUriString = path
            Toast.makeText(context, "照片拍摄完成！", Toast.LENGTH_SHORT).show()
            if (isPendingPublishAfterPhoto) {
                isPendingPublishAfterPhoto = false
                doSubmitPost(path)
            }
        } else {
            isPendingPublishAfterPhoto = false
        }
    }

    val triggerCamera = {
        val (file, uri) = createCapturedImageUri(context)
        capturedImageFile = file
        takePictureLauncher.launch(uri)
    }

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
                Text("当前状态", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("还在原地", "由发布者代为保管").forEach { status ->
                        SelectChip(status, selected = custodyStatus == status) {
                            custodyStatus = status
                            error = ""
                        }
                    }
                }
                if (custodyStatus == "由发布者代为保管") {
                    Text(
                        "💡 您已选择代为保管，可跳过拍照与具体地点要求，系统仍将自动记录当前经纬度",
                        color = BrandBlue,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                } else {
                    Text(
                        "📌 卡片留在原地时，请拍摄现场照片并填写具体地点，方便失主前往寻找",
                        color = ScreenMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
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
                label = if (custodyStatus == "还在原地") "具体地点" else "具体地点（代为保管可选）",
                value = location,
                onValueChange = { location = it; error = "" },
                placeholder = if (isLocating) "正在定位当前位置…" else if (custodyStatus == "由发布者代为保管") "代为保管可不填，已自动获取经纬度" else "例如：第一教学楼附近",
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
            if (currentLatitude != null && currentLongitude != null) {
                Text(
                    "📍 已自动获取经纬度：北纬 ${"%.4f".format(currentLatitude)}°, 东经 ${"%.4f".format(currentLongitude)}°",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (custodyStatus == "还在原地") "物品当前位置照片（必拍）" else "物品照片（代为保管可选）",
                    fontSize = 13.sp,
                    color = ScreenInk,
                    fontWeight = FontWeight.SemiBold
                )
                if (capturedImageUriString.isNotBlank()) {
                    Text(
                        "重新拍摄",
                        color = BrandBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { triggerCamera() }
                    )
                }
            }
            if (capturedImageUriString.isNotBlank()) {
                val bmp = remember(capturedImageUriString) { loadBitmapFromUriOrPath(context, capturedImageUriString) }
                if (bmp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "物品当前位置照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { triggerCamera() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📷", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (custodyStatus == "还在原地") "拍摄物品当前放置位置照片" else "拍摄物品照片（可选）",
                                color = ScreenInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (custodyStatus == "还在原地") "卡片留在原地，点击发布时将自动调用相机拍照" else "已选择代为保管，无需强制拍照，点击发布将直接跳过拍照",
                                color = ScreenMuted,
                                fontSize = 11.sp
                            )
                        }
                        Text("拍照 ›", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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
            if (documentIdentifier.isBlank() || personName.isBlank() || campus.isBlank() || foundDate.isBlank()) {
                error = "请填写完整号码、姓名、校区和拾获时间。"
                return@PrimaryActionButton
            }
            if (custodyStatus == "还在原地" && location.isBlank()) {
                error = "卡片还在原地，请填写具体地点方便失主寻找。"
                return@PrimaryActionButton
            }
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

            if (custodyStatus == "还在原地" && capturedImageUriString.isBlank()) {
                isPendingPublishAfterPhoto = true
                triggerCamera()
            } else {
                doSubmitPost(capturedImageUriString)
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
