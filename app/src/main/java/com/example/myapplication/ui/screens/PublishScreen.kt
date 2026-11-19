package com.example.myapplication.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.myapplication.util.NetworkTimeHelper
import com.kyant.backdrop.backdrops.LayerBackdrop
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PublishScreen(
    backdrop: LayerBackdrop,
    currentUser: UserProfile?,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onResetPassword: (String, String, String, (EduroamResult) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onUpdateUserContact: (String, String, String) -> Unit = { _, _, _ -> },
    onSubmit: (LostFoundPost) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }

    var documentIdentifier by remember { mutableStateOf("") }
    var personName by remember { mutableStateOf("") }
    var selectedDocumentName by rememberSaveable { mutableStateOf("校园卡") }
    var campus by rememberSaveable { mutableStateOf(currentUser?.campus?.ifBlank { "江安校区" } ?: "江安校区") }
    var location by rememberSaveable { mutableStateOf("") }
    var currentLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    var capturedImageUriString by rememberSaveable { mutableStateOf("") }
    var isPendingPublishAfterPhoto by remember { mutableStateOf(false) }

    var foundTimestamp by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var foundDate by rememberSaveable { mutableStateOf(formatCurrentDateTime()) }
    var custodyStatus by rememberSaveable { mutableStateOf("还在原地") }
    var sharePhoneInSitu by rememberSaveable { mutableStateOf(false) }
    var showPhoneSupplementDialog by remember { mutableStateOf(false) }
    var supplementPhoneInput by remember { mutableStateOf("") }
    var supplementPhoneError by remember { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    val selectedDocument = documentTypeOptions.first { it.name == selectedDocumentName }

    val showDateTimePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = foundTimestamp }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val selectedCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val selectedMillis = selectedCal.timeInMillis

                        // 异步调用腾讯时间接口校验所选时间不晚于当前时间
                        coroutineScope.launch {
                            val isValid = NetworkTimeHelper.validateNotLaterThanNow(selectedMillis)
                            if (!isValid) {
                                error = "拾获时间不能晚于当前时间，请重新选择"
                                Toast.makeText(context, "拾获时间不能晚于当前时间", Toast.LENGTH_SHORT).show()
                            } else {
                                error = ""
                                foundTimestamp = selectedMillis
                                val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
                                foundDate = formatter.format(Date(selectedMillis))
                            }
                        }
                    },
                    currentHour,
                    currentMinute,
                    true
                )
                timePickerDialog.show()
            },
            currentYear,
            currentMonth,
            currentDay
        )
        // 限制最大可选日期为今天
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

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
                        if (result.campus != null) {
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
        } else if (custodyStatus != "还在原地") {
            "由发布者代为保管"
        } else {
            "具体地点未填写"
        }

        val finalImagePath = if (custodyStatus != "还在原地") "" else imagePath

        val finalPhone = if (custodyStatus == "由我代为保管") {
            currentUser?.phone?.trim().orEmpty()
        } else {
            if (sharePhoneInSitu) currentUser?.phone?.trim().orEmpty() else ""
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
                imageUri = finalImagePath,
                contactPhone = finalPhone
            )
        )
        documentIdentifier = ""
        personName = ""
        location = ""
        error = ""
        capturedImageUriString = ""
        capturedImageFile = null
        foundTimestamp = System.currentTimeMillis()
        foundDate = formatCurrentDateTime()
        custodyStatus = "还在原地"
        sharePhoneInSitu = false
        campus = currentUser?.campus?.ifBlank { "江安校区" } ?: "江安校区"
        Toast.makeText(context, "发布成功！已上传经纬度位置${if (finalImagePath.isNotBlank()) "与现场照片" else ""}", Toast.LENGTH_SHORT).show()
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
                        Text("登记拾获信息需要登录，点击立即登录 / 注册", color = Color(0xFFEA580C), fontSize = 11.sp)
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
                label = selectedDocument.personNameLabel,
                value = personName,
                onValueChange = { personName = it; error = "" },
                placeholder = selectedDocument.personNamePlaceholder
            )
            FormTextField(
                label = selectedDocument.identifierLabel,
                value = documentIdentifier,
                onValueChange = { documentIdentifier = it; error = "" },
                placeholder = selectedDocument.identifierPlaceholder
            )
        }

        InfoCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("当前状态", fontSize = 13.sp, color = ScreenInk, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("还在原地", "由我代为保管").forEach { status ->
                        SelectChip(status, selected = custodyStatus == status) {
                            custodyStatus = status
                            error = ""
                            if (status == "由我代为保管" && (currentUser?.phone?.isBlank() == true)) {
                                supplementPhoneInput = ""
                                supplementPhoneError = ""
                                showPhoneSupplementDialog = true
                            }
                        }
                    }
                }
            }

            if (custodyStatus == "由我代为保管") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🛡️", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (currentUser?.phone?.isNotBlank() == true) {
                                    "已关联您的联系电话：${currentUser.phone}"
                                } else {
                                    "尚未登记联系电话（代保管必须填写）"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentUser?.phone?.isNotBlank() == true) SuccessGreen else Color(0xFFC2410C)
                            )
                            Text(
                                "号码仅在系统实名档案与卡片100%匹配时对唯一失主可见，不会对外公开。",
                                fontSize = 11.sp,
                                color = ScreenMuted
                            )
                        }
                        if (currentUser?.phone?.isBlank() == true) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandBlue,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                                    supplementPhoneInput = ""
                                    supplementPhoneError = ""
                                    showPhoneSupplementDialog = true
                                }
                            ) {
                                Text(
                                    "去填写",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!sharePhoneInSitu) {
                                    if (currentUser?.phone?.isBlank() == true) {
                                        supplementPhoneInput = ""
                                        supplementPhoneError = ""
                                        showPhoneSupplementDialog = true
                                    } else {
                                        sharePhoneInSitu = true
                                    }
                                } else {
                                    sharePhoneInSitu = false
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (sharePhoneInSitu) BrandBlue else Color.White,
                            border = BorderStroke(1.5.dp, if (sharePhoneInSitu) BrandBlue else ScreenMuted.copy(alpha = 0.5f)),
                            modifier = Modifier.size(18.dp)
                        ) {
                            if (sharePhoneInSitu) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "向失主公开我的联系电话（选填，默认不公开）",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScreenInk
                            )
                            Text(
                                if (currentUser?.phone?.isNotBlank() == true) {
                                    "勾选后，匹配失主认领时可查看到您的电话：${currentUser.phone}"
                                } else {
                                    "您尚未绑定手机号，勾选后将引导您快速补充"
                                },
                                fontSize = 11.sp,
                                color = ScreenMuted
                            )
                        }
                    }
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
            if (custodyStatus == "还在原地") {
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
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "拾获时间",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScreenInk
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0x18000000)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = showDateTimePicker)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🕒", fontSize = 16.sp)
                            Text(
                                foundDate,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScreenInk
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "选择时间 ›",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (currentLatitude != null && currentLongitude != null) {
                Text(
                    "北纬 ${"%.4f".format(currentLatitude)}°, 东经 ${"%.4f".format(currentLongitude)}°",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (custodyStatus == "还在原地") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "物品当前位置照片",
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
                                    "拍摄物品当前放置位置照片",
                                    color = ScreenInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "卡片留在原地，点击发布时将自动调用相机拍照",
                                    color = ScreenMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Text("拍照 ›", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFB5473C), fontSize = 13.sp)
        }
        PrimaryActionButton("发布") {
            if (currentUser == null) {
                showAuthDialog = true
                return@PrimaryActionButton
            }
            if (custodyStatus == "由我代为保管" && currentUser.phone.isBlank()) {
                supplementPhoneInput = ""
                supplementPhoneError = ""
                showPhoneSupplementDialog = true
                error = "由于物品由您代为保管，请先补充联系电话以便失主联系。"
                return@PrimaryActionButton
            }
            if (documentIdentifier.isBlank() || personName.isBlank() || campus.isBlank() || foundDate.isBlank()) {
                error = "请填写完整号码、姓名、校区和拾获时间。"
                return@PrimaryActionButton
            }
            if (custodyStatus == "还在原地") {
                if (location.isBlank()) {
                    error = "卡片还在原地，请填写具体地点方便失主寻找。"
                    return@PrimaryActionButton
                }
                if (capturedImageUriString.isBlank()) {
                    isPendingPublishAfterPhoto = true
                    triggerCamera()
                    return@PrimaryActionButton
                }
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

            val finalImagePath = if (custodyStatus != "还在原地") "" else capturedImageUriString
            coroutineScope.launch {
                val isTimeValid = NetworkTimeHelper.validateNotLaterThanNow(foundTimestamp)
                if (!isTimeValid) {
                    error = "所选拾获时间不能晚于当前时间，请重新选择"
                    return@launch
                }
                doSubmitPost(finalImagePath)
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

    if (showPhoneSupplementDialog && currentUser != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPhoneSupplementDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📱", fontSize = 20.sp)
                        Column {
                            Text("补充联系电话", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScreenInk)
                            Text("方便失主与您取得联系进行交接", fontSize = 11.sp, color = ScreenMuted)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "提示：联系电话将被安全加密存储，且仅在失主实名信息与拾获卡片 100% 匹配成功后，对其单独显示。",
                            color = BrandBlue,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    FormTextField(
                        label = "手机号码",
                        value = supplementPhoneInput,
                        onValueChange = {
                            supplementPhoneInput = it
                            supplementPhoneError = ""
                        },
                        placeholder = "请输入11位中国大陆手机号码"
                    )

                    if (supplementPhoneError.isNotBlank()) {
                        Text(supplementPhoneError, color = Color(0xFFB5473C), fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f).clickable {
                                showPhoneSupplementDialog = false
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text("取消", color = ScreenMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BrandBlue,
                            modifier = Modifier.weight(1.3f).clickable {
                                val phoneErr = ValidationHelper.validatePhone(supplementPhoneInput, isOptional = false)
                                if (phoneErr != null) {
                                    supplementPhoneError = phoneErr
                                    return@clickable
                                }
                                val cleanPhone = supplementPhoneInput.replace(" ", "").trim()
                                onUpdateUserContact(currentUser.studentId, cleanPhone, currentUser.campus)
                                showPhoneSupplementDialog = false
                                if (custodyStatus == "还在原地") {
                                    sharePhoneInSitu = true
                                }
                                error = ""
                                Toast.makeText(context, "联系电话保存成功！", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text("确认保存", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
