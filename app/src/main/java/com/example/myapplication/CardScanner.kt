package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraController
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
internal fun CardScannerDialog(
    onDismiss: () -> Unit,
    onImageCaptured: (Uri, File) -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf("") }

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionRequest.launch(Manifest.permission.CAMERA)
    }

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    DisposableEffect(cameraController, lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) cameraController.bindToLifecycle(lifecycleOwner)
        onDispose { cameraController.unbind() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            controller = cameraController
                        }
                    },
                    update = { it.controller = cameraController }
                )
                CardTargetOverlay(Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("需要相机权限才能扫描卡片", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    ScannerAction("允许相机") {
                        permissionRequest.launch(Manifest.permission.CAMERA)
                    }
                    ScannerAction("从相册选择") { galleryPicker.launch("image/*") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "扫描卡片",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "关闭",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp)
                )
            }

            if (hasCameraPermission) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    if (cameraError.isNotBlank()) {
                        Text(cameraError, color = Color.White, fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(74.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "相册",
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f).clickable(enabled = !isCapturing) {
                                galleryPicker.launch("image/*")
                            }.padding(vertical = 18.dp)
                        )
                        Box(
                            modifier = Modifier.size(68.dp).background(Color.White, CircleShape)
                                .padding(5.dp).background(Color(0xFF2878C8), CircleShape)
                                .clickable(enabled = !isCapturing) {
                                    val directory = File(context.cacheDir, "ocr")
                                    if (!directory.exists()) directory.mkdirs()
                                    val imageFile = File.createTempFile("card-scan-", ".jpg", directory)
                                    val outputUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    val output = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                                    isCapturing = true
                                    cameraError = ""
                                    try {
                                        cameraController.takePicture(
                                            output,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    isCapturing = false
                                                    onImageCaptured(outputUri, imageFile)
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    imageFile.delete()
                                                    isCapturing = false
                                                    cameraError = exception.message ?: "拍摄失败，请重试。"
                                                }
                                            }
                                        )
                                    } catch (exception: Exception) {
                                        imageFile.delete()
                                        isCapturing = false
                                        cameraError = exception.localizedMessage ?: "相机暂不可用，请重试。"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                Text("…", color = Color.White, fontSize = 24.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTargetOverlay(modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val desiredWidth = size.width * 0.88f
            val frameWidth = minOf(desiredWidth, size.height * 0.50f * 1.586f)
            val frameHeight = frameWidth / 1.586f
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f
            val frameRect = Rect(left, top, left + frameWidth, top + frameHeight)
            val radius = 16.dp.toPx()
            val spotlight = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(frameRect, CornerRadius(radius, radius)))
            }
            drawPath(spotlight, Color.Black.copy(alpha = 0.56f))
            drawRoundRect(
                color = Color.White,
                topLeft = frameRect.topLeft,
                size = frameRect.size,
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 2.5.dp.toPx())
            )
            val cornerLength = frameWidth * 0.075f
            val cornerStroke = 5.dp.toPx()
            listOf(
                frameRect.topLeft to listOf(
                    frameRect.topLeft + androidx.compose.ui.geometry.Offset(cornerLength, 0f),
                    frameRect.topLeft + androidx.compose.ui.geometry.Offset(0f, cornerLength)
                ),
                androidx.compose.ui.geometry.Offset(frameRect.right, frameRect.top) to listOf(
                    androidx.compose.ui.geometry.Offset(frameRect.right - cornerLength, frameRect.top),
                    androidx.compose.ui.geometry.Offset(frameRect.right, frameRect.top + cornerLength)
                ),
                androidx.compose.ui.geometry.Offset(frameRect.left, frameRect.bottom) to listOf(
                    androidx.compose.ui.geometry.Offset(frameRect.left + cornerLength, frameRect.bottom),
                    androidx.compose.ui.geometry.Offset(frameRect.left, frameRect.bottom - cornerLength)
                ),
                frameRect.bottomRight to listOf(
                    frameRect.bottomRight + androidx.compose.ui.geometry.Offset(-cornerLength, 0f),
                    frameRect.bottomRight + androidx.compose.ui.geometry.Offset(0f, -cornerLength)
                )
            ).forEach { (start, ends) ->
                ends.forEach { end -> drawLine(Color(0xFF6DB8FF), start, end, cornerStroke) }
            }
        }
        Text(
            "将卡片正面完整放入框内，保持清晰",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 106.dp)
        )
    }
}

@Composable
private fun ScannerAction(title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.16f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
    }
}
