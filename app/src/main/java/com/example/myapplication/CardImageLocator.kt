package com.example.myapplication

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max

/** Finds and rectifies a card; falls back to the scanner's centered frame crop when edges are weak. */
internal object CardImageLocator {
    private const val maxWorkingDimension = 1800
    private const val minCardAreaRatio = 0.012
    private const val minCardAspectRatio = 1.0
    private const val maxCardAspectRatio = 2.2
    private const val scannerFrameAspectRatio = 1.586
    private const val rectifiedWidth = 1920
    private const val rectifiedHeight = 1210

    fun locateAndRectify(bitmap: Bitmap): Bitmap {
        check(OpenCVLoader.initLocal()) { "OpenCV 初始化失败，无法定位卡片。" }

        val scale = max(bitmap.width, bitmap.height).toFloat() / maxWorkingDimension
        val workingBitmap = if (scale > 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width / scale).toInt().coerceAtLeast(1),
                (bitmap.height / scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val rgba = Mat()
        val gray = Mat()
        val blurred = Mat()
        val edges = Mat()
        val closedEdges = Mat()
        val thresholded = Mat()
        val hierarchy = Mat()
        val morphologyKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
        var sourcePoints: MatOfPoint2f? = null
        var targetPoints: MatOfPoint2f? = null
        var transform: Mat? = null
        val warped = Mat()

        try {
            Utils.bitmapToMat(workingBitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val cardQuad = findCardQuad(
                blurred,
                workingBitmap.width,
                workingBitmap.height,
                edges,
                closedEdges,
                thresholded,
                hierarchy,
                morphologyKernel
            )
            if (cardQuad == null) {
                return centerFrameCrop(workingBitmap)
            }

            sourcePoints = MatOfPoint2f(*cardQuad.corners)
            targetPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point((rectifiedWidth - 1).toDouble(), 0.0),
                Point((rectifiedWidth - 1).toDouble(), (rectifiedHeight - 1).toDouble()),
                Point(0.0, (rectifiedHeight - 1).toDouble())
            )
            transform = Imgproc.getPerspectiveTransform(sourcePoints, targetPoints)
            Imgproc.warpPerspective(
                rgba,
                warped,
                transform,
                Size(rectifiedWidth.toDouble(), rectifiedHeight.toDouble()),
                Imgproc.INTER_CUBIC
            )
            return Bitmap.createBitmap(rectifiedWidth, rectifiedHeight, Bitmap.Config.ARGB_8888).also {
                Utils.matToBitmap(warped, it)
            }
        } finally {
            if (workingBitmap !== bitmap) workingBitmap.recycle()
            rgba.release()
            gray.release()
            blurred.release()
            edges.release()
            closedEdges.release()
            thresholded.release()
            hierarchy.release()
            morphologyKernel.release()
            sourcePoints?.release()
            targetPoints?.release()
            transform?.release()
            warped.release()
        }
    }

    private data class CardQuad(val corners: Array<Point>, val score: Double)

    private fun findCardQuad(
        gray: Mat,
        imageWidth: Int,
        imageHeight: Int,
        edges: Mat,
        closedEdges: Mat,
        thresholded: Mat,
        hierarchy: Mat,
        morphologyKernel: Mat
    ): CardQuad? {
        val bestCandidates = mutableListOf<CardQuad>()
        val edgeThresholds = listOf(24.0 to 72.0, 40.0 to 120.0, 64.0 to 192.0)
        edgeThresholds.forEach { (low, high) ->
            Imgproc.Canny(gray, edges, low, high)
            Imgproc.morphologyEx(edges, closedEdges, Imgproc.MORPH_CLOSE, morphologyKernel)
            collectCandidates(closedEdges, imageWidth, imageHeight, hierarchy, bestCandidates)
        }

        listOf(Imgproc.THRESH_BINARY, Imgproc.THRESH_BINARY_INV).forEach { thresholdMode ->
            Imgproc.adaptiveThreshold(
                gray,
                thresholded,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                thresholdMode,
                31,
                7.0
            )
            Imgproc.morphologyEx(thresholded, closedEdges, Imgproc.MORPH_CLOSE, morphologyKernel)
            collectCandidates(closedEdges, imageWidth, imageHeight, hierarchy, bestCandidates)
        }

        return bestCandidates.maxByOrNull(CardQuad::score)
    }

    private fun collectCandidates(
        mask: Mat,
        imageWidth: Int,
        imageHeight: Int,
        hierarchy: Mat,
        results: MutableList<CardQuad>
    ) {
        val input = mask.clone()
        val contours = arrayListOf<MatOfPoint>()
        try {
            Imgproc.findContours(input, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val imageArea = imageWidth.toDouble() * imageHeight
            contours.forEach { contour ->
                findCardQuads(contour, imageArea, imageWidth, imageHeight).forEach(results::add)
            }
        } finally {
            input.release()
            contours.forEach { it.release() }
        }
    }

    private fun findCardQuads(
        contour: MatOfPoint,
        imageArea: Double,
        imageWidth: Int,
        imageHeight: Int
    ): List<CardQuad> {
        val contourArea = Imgproc.contourArea(contour)
        if (contourArea < imageArea * minCardAreaRatio || contourArea > imageArea * 0.96) return emptyList()

        val contourPoints = MatOfPoint2f(*contour.toArray())
        val approximate = MatOfPoint2f()
        try {
            val perimeter = Imgproc.arcLength(contourPoints, true)
            if (perimeter <= 0.0) return emptyList()
            val candidates = mutableListOf<CardQuad>()
            listOf(0.012, 0.018, 0.026, 0.038, 0.052).forEach { epsilonRatio ->
                Imgproc.approxPolyDP(contourPoints, approximate, perimeter * epsilonRatio, true)
                val points = approximate.toArray()
                if (points.size != 4) return@forEach

                val polygon = MatOfPoint(*points)
                val isConvex = try {
                    Imgproc.isContourConvex(polygon)
                } finally {
                    polygon.release()
                }
                if (!isConvex) return@forEach

                val ordered = orderCorners(points) ?: return@forEach
                val minX = ordered.minOf { it.x }
                val maxX = ordered.maxOf { it.x }
                val minY = ordered.minOf { it.y }
                val maxY = ordered.maxOf { it.y }
                if ((maxX - minX) >= imageWidth * 0.98 && (maxY - minY) >= imageHeight * 0.98) {
                    return@forEach
                }

                val top = distance(ordered[0], ordered[1])
                val bottom = distance(ordered[3], ordered[2])
                val left = distance(ordered[0], ordered[3])
                val right = distance(ordered[1], ordered[2])
                val aspectRatio = max(top, bottom) / max(left, right).coerceAtLeast(1.0)
                if (aspectRatio !in minCardAspectRatio..maxCardAspectRatio) return@forEach

                val centerX = (minX + maxX) / 2.0
                val centerY = (minY + maxY) / 2.0
                val offsetX = (centerX - imageWidth / 2.0) / imageWidth
                val offsetY = (centerY - imageHeight / 2.0) / imageHeight
                val centerPenalty = 1.0 + 5.0 * (offsetX * offsetX + offsetY * offsetY)
                candidates += CardQuad(ordered, contourArea / centerPenalty)
            }
            return candidates
        } finally {
            contourPoints.release()
            approximate.release()
        }
    }

    private fun centerFrameCrop(bitmap: Bitmap): Bitmap {
        val cropWidth = minOf(
            bitmap.width * 0.86,
            bitmap.height * 0.86 * scannerFrameAspectRatio
        ).toInt().coerceAtLeast(1)
        val cropHeight = (cropWidth / scannerFrameAspectRatio).toInt().coerceAtLeast(1)
            .coerceAtMost(bitmap.height)
        val cropLeft = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
        val cropTop = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
        val crop = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
        return try {
            Bitmap.createScaledBitmap(crop, rectifiedWidth, rectifiedHeight, true)
        } finally {
            if (crop !== bitmap) crop.recycle()
        }
    }

    private fun orderCorners(points: Array<Point>): Array<Point>? {
        val topLeft = points.minByOrNull { it.x + it.y } ?: return null
        val bottomRight = points.maxByOrNull { it.x + it.y } ?: return null
        val topRight = points.maxByOrNull { it.x - it.y } ?: return null
        val bottomLeft = points.minByOrNull { it.x - it.y } ?: return null
        val ordered = arrayOf(topLeft, topRight, bottomRight, bottomLeft)
        return if (ordered.toSet().size == 4) ordered else null
    }

    private fun distance(first: Point, second: Point): Double =
        hypot(first.x - second.x, first.y - second.y)
}
