package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Local-only OCR for printed text on a found campus card. */
internal object CardOcr {
    private const val language = "chi_sim"
    private const val modelUrl =
        "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/chi_sim.traineddata"
    private const val minimumModelBytes = 1_000_000L
    private const val maxImageDimension = 2400

    suspend fun readCardSuffix(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        val dataDirectory = File(context.filesDir, "tesseract")
        val tessdataDirectory = File(dataDirectory, "tessdata")
        val model = File(tessdataDirectory, "$language.traineddata")
        if (!model.exists() || model.length() < minimumModelBytes) {
            downloadModel(model)
        }

        val bitmap = decodeBitmap(context, imageUri)
        val api = TessBaseAPI()
        try {
            if (!api.init(dataDirectory.absolutePath, language)) {
                throw IllegalStateException("中文识别模型初始化失败。")
            }
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            api.setImage(bitmap)
            extractLongestDigitRun(api.getUTF8Text().orEmpty())
        } finally {
            api.recycle()
            bitmap.recycle()
        }
    }

    private fun downloadModel(destination: File) {
        val parentDirectory = requireNotNull(destination.parentFile)
        parentDirectory.mkdirs()
        val temporary = File(parentDirectory, "${destination.name}.download")
        val connection = URL(modelUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("中文识别模型下载失败（HTTP ${connection.responseCode}）。")
            }
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                }
            }
            if (temporary.length() < minimumModelBytes) {
                throw IllegalStateException("下载的中文识别模型不完整，请重试。")
            }
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("无法更新中文识别模型。")
            }
            if (!temporary.renameTo(destination)) {
                throw IllegalStateException("无法保存中文识别模型。")
            }
        } finally {
            connection.disconnect()
            temporary.delete()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalStateException("无法读取所选图片。")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("所选文件不是有效图片。")
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxImageDimension) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("无法解码所选图片。")
        val rotation = readRotation(context, uri)
        if (rotation == 0) return bitmap

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(rotation.toFloat()) },
            true
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun readRotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }

    private fun extractLongestDigitRun(text: String): String? =
        Regex("(?:\\d[\\s-]?){4,}")
            .findAll(text)
            .map { match -> match.value.filter(Char::isDigit) }
            .filter { it.length >= 4 }
            .maxByOrNull(String::length)
            ?.takeLast(4)
}
}
