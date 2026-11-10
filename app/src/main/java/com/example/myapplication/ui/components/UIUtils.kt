package com.example.myapplication.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.myapplication.data.model.LostFoundPost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createCapturedImageUri(context: Context): Pair<File, Uri> {
    val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
    val file = File(imagesDir, "card_item_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    return Pair(file, uri)
}

fun loadBitmapFromUriOrPath(context: Context, imageUri: String): Bitmap? {
    if (imageUri.isBlank()) return null
    return try {
        val uri = Uri.parse(imageUri)
        if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } else {
            BitmapFactory.decodeFile(imageUri)
        }
    } catch (_: Exception) {
        null
    }
}

fun openMapWithCoordinates(context: Context, latitude: Double, longitude: Double, label: String) {
    try {
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        try {
            val webUri = Uri.parse("https://uri.amap.com/marker?position=$longitude,$latitude&name=${Uri.encode(label)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "未能打开地图导航", Toast.LENGTH_SHORT).show()
        }
    }
}

fun getPostDisplayTitle(post: LostFoundPost): String {
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

fun formatCurrentDateTime(): String {
    val formatter = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

val demoPosts = listOf(
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
        documentNumber = "2023141440001",
        latitude = 30.5563,
        longitude = 103.9965
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
        documentNumber = "510107200305011234",
        latitude = 30.6318,
        longitude = 104.0842
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
        documentNumber = "EE12345678",
        latitude = 30.6436,
        longitude = 104.0624
    )
)
