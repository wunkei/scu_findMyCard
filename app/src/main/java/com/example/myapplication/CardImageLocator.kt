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

/** Finds the largest card-shaped quadrilateral and corrects its perspective. */
internal object CardImageLocator {
    private const val maxWorkingDimension = 1800
    private const val minCardAreaRatio = 0.035
    private const val minCardAspectRatio = 1.28
    private const val maxCardAspectRatio = 2.05
    private const val rectifiedWidth = 1280
    private const val rectifiedHeight = 807

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
        val hierarchy = Mat()
        val contours = arrayListOf<MatOfPoint>()
        var contourInput: Mat? = null
        var sourcePoints: MatOfPoint2f? = null
        var targetPoints: MatOfPoint2f? = null
        var transform: Mat? = null
        val warped = Mat()

        try {
            Utils.bitmapToMat(workingBitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 48.0, 144.0)
            contourInput = edges.clone()
            Imgproc.findContours(
                contourInput,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            val imageArea = workingBitmap.width.toDouble() * workingBitmap.height
            val cardCorners = contours
                .asSequence()
                .mapNotNull { contour ->
                    findCardQuad(contour, imageArea, workingBitmap.width, workingBitmap.height)
                }
                .maxByOrNull { it.area }
                ?.corners
                ?: throw IllegalStateException("没有找到完整卡片，请让卡片四角都进入取景框后重试。")

            sourcePoints = MatOfPoint2f(*cardCorners)
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
            hierarchy.release()
            contourInput?.release()
            contours.forEach { it.release() }
            sourcePoints?.release()
            targetPoints?.release()
            transform?.release()
            warped.release()
        }
    }

    private data class CardQuad(val corners: Array<Point>, val area: Double)

    private fun findCardQuad(
        contour: MatOfPoint,
        imageArea: Double,
        imageWidth: Int,
        imageHeight: Int
    ): CardQuad? {
        val contourArea = Imgproc.contourArea(contour)
        if (contourArea < imageArea * minCardAreaRatio) return null

        val contourPoints = MatOfPoint2f(*contour.toArray())
        val approximate = MatOfPoint2f()
        try {
            val perimeter = Imgproc.arcLength(contourPoints, true)
            Imgproc.approxPolyDP(contourPoints, approximate, perimeter * 0.018, true)
            val points = approximate.toArray()
            if (points.size != 4) return null

            val polygon = MatOfPoint(*points)
            val isConvex = try {
                Imgproc.isContourConvex(polygon)
            } finally {
                polygon.release()
            }
            if (!isConvex) return null

            val ordered = orderCorners(points) ?: return null
            val horizontalMargin = imageWidth * 0.015
            val verticalMargin = imageHeight * 0.015
            if (ordered.any {
                    it.x <= horizontalMargin || it.x >= imageWidth - horizontalMargin ||
                        it.y <= verticalMargin || it.y >= imageHeight - verticalMargin
                }
            ) return null

            val top = distance(ordered[0], ordered[1])
            val bottom = distance(ordered[3], ordered[2])
            val left = distance(ordered[0], ordered[3])
            val right = distance(ordered[1], ordered[2])
            val aspectRatio = max(top, bottom) / max(left, right).coerceAtLeast(1.0)
            if (aspectRatio !in minCardAspectRatio..maxCardAspectRatio) return null

            return CardQuad(ordered, contourArea)
        } finally {
            contourPoints.release()
            approximate.release()
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
