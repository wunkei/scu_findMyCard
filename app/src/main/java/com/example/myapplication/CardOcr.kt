package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import com.googlecode.tesseract.android.ResultIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

internal enum class RecognizedCardType(val title: String) {
    CAMPUS_CARD("校园卡"),
    IDENTITY_CARD("居民身份证"),
    HOME_RETURN_PERMIT("港澳居民来往内地通行证"),
    OTHER("暂无法判断")
}

internal data class CardTextLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float
) {
    val centerY: Float get() = (top + bottom) / 2f
}

internal data class CardScanResult(
    val type: RecognizedCardType,
    val typeConfidence: Float,
    val matchedKeyword: String?,
    val lines: List<CardTextLine>,
    val cardNumberSuffix: String?
)

/** Local card-boundary detection, text OCR, and keyword/position classification. */
internal object CardOcr {
    private const val language = "chi_sim"
    private const val minimumModelBytes = 1_000_000L
    private const val maxImageDimension = 2400

    suspend fun scan(context: Context, imageUri: Uri): CardScanResult = withContext(Dispatchers.IO) {
        val dataDirectory = File(context.filesDir, "tesseract")
        val tessdataDirectory = File(dataDirectory, "tessdata")
        val model = File(tessdataDirectory, "$language.traineddata")
        if (!model.exists() || model.length() < minimumModelBytes) {
            installBundledModel(context, model)
        }

        val originalBitmap = decodeBitmap(context, imageUri)
        var rectifiedBitmap: Bitmap? = null
        try {
            rectifiedBitmap = CardImageLocator.locateAndRectify(originalBitmap)
            val textLines = recognizeText(dataDirectory, rectifiedBitmap)
            val classification = classifyCard(textLines)
            CardScanResult(
                type = classification.type,
                typeConfidence = classification.confidence,
                matchedKeyword = classification.keyword,
                lines = textLines,
                cardNumberSuffix = if (classification.type == RecognizedCardType.CAMPUS_CARD) {
                    extractCardNumberSuffix(textLines)
                } else {
                    null
                }
            )
        } finally {
            rectifiedBitmap?.recycle()
            originalBitmap.recycle()
        }
    }

    private fun recognizeText(dataDirectory: File, bitmap: Bitmap): List<CardTextLine> {
        val api = TessBaseAPI()
        try {
            if (!api.init(dataDirectory.absolutePath, language)) {
                throw IllegalStateException("中文识别模型初始化失败。")
            }
            api.setVariable("user_defined_dpi", "300")

            val pageModes = listOf(
                TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT,
                TessBaseAPI.PageSegMode.PSM_AUTO,
                TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
            )
            for (pageMode in pageModes) {
                api.setPageSegMode(pageMode)
                api.setImage(bitmap)

                // getUTF8Text() triggers recognition; only then request the positioned iterator.
                val recognizedText = api.getUTF8Text().orEmpty()
                val positionedLines = readPositionedLines(api, bitmap)
                if (positionedLines.isNotEmpty()) return positionedLines

                val plainTextLines = fallbackTextLines(recognizedText)
                if (plainTextLines.isNotEmpty()) return plainTextLines
            }
            return emptyList()
        } finally {
            api.recycle()
        }
    }

    private fun readPositionedLines(api: TessBaseAPI, bitmap: Bitmap): List<CardTextLine> {
        val iterator: ResultIterator = api.getResultIterator() ?: return emptyList()
        try {
            val results = mutableListOf<CardTextLine>()
            iterator.begin()
            do {
                val text = iterator.getUTF8Text(PageIteratorLevel.RIL_TEXTLINE)
                    ?.trim()
                    .orEmpty()
                val bounds = iterator.getBoundingRect(PageIteratorLevel.RIL_TEXTLINE)
                if (text.isNotBlank() && bounds.width() > 0 && bounds.height() > 0) {
                    results += CardTextLine(
                        text = text,
                        left = bounds.left.toFloat() / bitmap.width,
                        top = bounds.top.toFloat() / bitmap.height,
                        right = bounds.right.toFloat() / bitmap.width,
                        bottom = bounds.bottom.toFloat() / bitmap.height,
                        confidence = (iterator.confidence(PageIteratorLevel.RIL_TEXTLINE) / 100f)
                            .coerceIn(0f, 1f)
                    )
                }
            } while (iterator.next(PageIteratorLevel.RIL_TEXTLINE))
            return results
        } finally {
            iterator.delete()
        }
    }

    private fun fallbackTextLines(text: String): List<CardTextLine> {
        val recognizedLines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (recognizedLines.isEmpty()) return emptyList()
        val lineHeight = 0.86f / recognizedLines.size
        return recognizedLines.mapIndexed { index, line ->
            val top = 0.06f + index * lineHeight
            CardTextLine(
                text = line,
                left = 0.03f,
                top = top,
                right = 0.97f,
                bottom = (top + lineHeight * 0.72f).coerceAtMost(0.98f),
                confidence = 0.35f
            )
        }
    }

    private data class CardClassification(
        val type: RecognizedCardType,
        val confidence: Float,
        val keyword: String?
    )

    private fun classifyCard(lines: List<CardTextLine>): CardClassification {
        fun normalize(text: String) = text.lowercase(Locale.ROOT)
            .replace(Regex("[\\s\\p{P}\\p{S}]"), "")

        val reliableLines = lines.filter { it.confidence >= 0.25f }
        val reliableText = normalize(reliableLines.joinToString("") { it.text })
        val upperText = normalize(
            reliableLines.filter { it.centerY <= 0.48f }.joinToString("") { it.text }
        )

        val permitKeywords = listOf(
            "港澳居民来往内地通行证",
            "港澳居民来往内地通行證",
            "回乡证",
            "回鄉證",
            "HOMERETURNPERMIT"
        )
        permitKeywords.firstOrNull { keyword ->
            val normalizedKeyword = normalize(keyword)
            upperText.contains(normalizedKeyword) ||
                lines.any {
                    it.confidence >= 0.25f && it.centerY <= 0.62f &&
                        normalize(it.text).contains(normalizedKeyword)
                }
        }?.let { keyword ->
            return CardClassification(RecognizedCardType.HOME_RETURN_PERMIT, .96f, keyword)
        }

        val identityKeywords = listOf(
            "中华人民共和国居民身份证",
            "居民身份证",
            "居民身分證",
            "身份证",
            "身分證",
            "IDENTITYCARD"
        )
        identityKeywords.firstOrNull { keyword ->
            val normalizedKeyword = normalize(keyword)
            upperText.contains(normalizedKeyword) ||
                lines.any {
                    it.confidence >= 0.25f && it.centerY <= 0.62f &&
                        normalize(it.text).contains(normalizedKeyword)
                }
        }?.let { keyword ->
            return CardClassification(RecognizedCardType.IDENTITY_CARD, .94f, keyword)
        }

        val explicitCampusKeywords = listOf(
            "校园一卡通",
            "校园卡",
            "学生卡",
            "一卡通",
            "CAMPUSCARD",
            "STUDENTCARD"
        )
        explicitCampusKeywords.firstOrNull { keyword ->
            lines.any { line ->
                line.confidence >= 0.25f && line.centerY <= 0.68f &&
                    normalize(line.text).contains(normalize(keyword))
            }
        }
            ?.let { keyword ->
                return CardClassification(RecognizedCardType.CAMPUS_CARD, .92f, keyword)
            }

        val universityKeywords = listOf("四川大学", "四川大學", "SICHUANUNIVERSITY", "SCU")
        val supportKeywords = listOf("学号", "學生", "学生", "校园", "卡号", "一卡通", "STUDENT")
        val universityAtTop = universityKeywords.firstOrNull { keyword ->
            lines.any {
                it.confidence >= 0.25f && it.centerY <= 0.42f &&
                    normalize(it.text).contains(normalize(keyword))
            }
        }
        val hasCampusContext = supportKeywords.any { normalize(it) in reliableText }
        if (universityAtTop != null && hasCampusContext) {
            return CardClassification(RecognizedCardType.CAMPUS_CARD, .80f, universityAtTop)
        }

        return CardClassification(RecognizedCardType.OTHER, .25f, null)
    }

    private fun extractCardNumberSuffix(lines: List<CardTextLine>): String? {
        val numberRegex = Regex("(?:\\d[\\s-]?){4,}")
        fun numbers(line: CardTextLine): List<String> = numberRegex.findAll(line.text)
            .map { it.value.filter(Char::isDigit) }
            .filter { it.length >= 4 }
            .toList()

        val labelLines = lines.filter { line ->
            listOf("学号", "學生證號", "卡号", "卡號", "校园卡号", "校园卡號")
                .any(line.text::contains) && !line.text.contains("电话") && !line.text.contains("電話")
        }
        val labelledNumber = labelLines.asSequence()
            .flatMap { label ->
                val sameLineNumbers = numbers(label)
                if (sameLineNumbers.isNotEmpty()) {
                    sameLineNumbers.asSequence()
                } else {
                    lines.asSequence()
                        .filter { candidate ->
                            candidate !== label &&
                                kotlin.math.abs(candidate.centerY - label.centerY) <= 0.12f &&
                                candidate.left >= label.left - 0.04f
                        }
                        .flatMap { numbers(it).asSequence() }
                }
            }
            .maxByOrNull(String::length)
        if (labelledNumber != null) return labelledNumber.takeLast(4)

        // Fallback only after the card has already been classified as a campus card.
        return lines.asSequence()
            .filter { line ->
                line.centerY in 0.16f..0.90f &&
                    !listOf("电话", "電話", "出生", "日期", "有效期", "期限")
                        .any(line.text::contains)
            }
            .flatMap { numbers(it).asSequence() }
            .filter { it.length in 6..20 }
            .maxByOrNull(String::length)
            ?.takeLast(4)
    }

    private fun installBundledModel(context: Context, destination: File) {
        val parentDirectory = requireNotNull(destination.parentFile)
        parentDirectory.mkdirs()
        val temporary = File(parentDirectory, "${destination.name}.download")
        try {
            context.assets.open("tessdata/$language.traineddata").use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                }
            }
            if (temporary.length() < minimumModelBytes) {
                throw IllegalStateException("安装包中的中文识别模型不完整。")
            }
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("无法更新中文识别模型。")
            }
            if (!temporary.renameTo(destination)) {
                throw IllegalStateException("无法准备中文识别模型。")
            }
        } finally {
            temporary.delete()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取所选图片。")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
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

}
