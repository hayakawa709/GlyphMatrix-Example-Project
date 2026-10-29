package com.kemonofriends.glyphtoy.speedometer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.kemonofriends.glyphtoy.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class SpeedometerFrameRenderer(context: Context) {

    private val baseBitmap: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.speedometer_base
    )

    fun renderAllOnPlaceholder(): Bitmap = render(0f)

    fun render(speedKmh: Float): Bitmap {
        val pixels = PixelCanvas(baseBitmap)
        val digitClearance = pixels.drawSpeedDigits(speedKmh)
        pixels.drawNeedle(speedKmh, digitClearance)
        return pixels.toBitmap()
    }

    private class PixelCanvas(baseBitmap: Bitmap) {
        private val pixels = Array(MATRIX_SIZE) { BooleanArray(MATRIX_SIZE) }

        init {
            for (y in 0 until minOf(MATRIX_SIZE, baseBitmap.height)) {
                for (x in 0 until minOf(MATRIX_SIZE, baseBitmap.width)) {
                    val color = baseBitmap.getPixel(x, y)
                    if (Color.alpha(color) > 0 && brightness(color) > 0) {
                        pixels[y][x] = true
                    }
                }
            }
        }

        fun drawNeedle(speedKmh: Float, digitClearance: Array<BooleanArray>) {
            val angle = speedToAngleDegrees(speedKmh)
            val tip = pointOnDial(angle, NEEDLE_TIP_RADIUS)
            val start = pointOnDial(angle, NEEDLE_START_RADIUS)
            drawLine(start.x, start.y, tip.x, tip.y, digitClearance)
        }

        fun drawSpeedDigits(speedKmh: Float): Array<BooleanArray> {
            val digitClearance = Array(MATRIX_SIZE) { BooleanArray(MATRIX_SIZE) }
            val speed = speedKmh.roundToInt().coerceIn(0, 999)
            val text = speed.toString().padStart(3, '0')
            text.forEachIndexed { index, digit ->
                drawGlyph(
                    DIGITS[digit] ?: return@forEachIndexed,
                    DIGIT_X[index],
                    DIGIT_Y,
                    digitClearance
                )
            }
            return digitClearance
        }

        fun toBitmap(): Bitmap {
            val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLACK)
            for (y in 0 until MATRIX_SIZE) {
                for (x in 0 until MATRIX_SIZE) {
                    if (pixels[y][x]) bitmap.setPixel(x, y, Color.WHITE)
                }
            }
            return bitmap
        }

        private fun drawGlyph(
            glyph: Array<String>,
            startX: Int,
            startY: Int,
            digitClearance: Array<BooleanArray>
        ) {
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { column, pixel ->
                    if (pixel == '1') {
                        val x = startX + column
                        val y = startY + row
                        set(x, y)
                        markDigitClearance(digitClearance, x, y)
                    }
                }
            }
        }

        private fun markDigitClearance(clearance: Array<BooleanArray>, x: Int, y: Int) {
            for (offsetY in -1..1) {
                for (offsetX in -1..1) {
                    val targetX = x + offsetX
                    val targetY = y + offsetY
                    if (targetX in 0 until MATRIX_SIZE && targetY in 0 until MATRIX_SIZE) {
                        clearance[targetY][targetX] = true
                    }
                }
            }
        }

        private fun drawLine(
            startX: Int,
            startY: Int,
            endX: Int,
            endY: Int,
            digitClearance: Array<BooleanArray>
        ) {
            var x0 = startX
            var y0 = startY
            val dx = abs(endX - x0)
            val dy = -abs(endY - y0)
            val sx = if (x0 < endX) 1 else -1
            val sy = if (y0 < endY) 1 else -1
            var error = dx + dy

            while (true) {
                if (!digitClearance[y0][x0]) set(x0, y0)
                if (x0 == endX && y0 == endY) break
                val e2 = 2 * error
                if (e2 >= dy) {
                    error += dy
                    x0 += sx
                }
                if (e2 <= dx) {
                    error += dx
                    y0 += sy
                }
            }
        }

        private fun set(x: Int, y: Int) {
            if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
                pixels[y][x] = true
            }
        }

        private fun speedToAngleDegrees(speedKmh: Float): Float {
            val progress = if (speedKmh > HIGH_SPEED_MODE_THRESHOLD_KMH) {
                // In high-speed mode, the dial's center is 100 km/h. The eight intervals are
                // 20, 40, 60, 80, 100, 120, 140, 160, and 180 km/h.
                (speedKmh.coerceIn(HIGH_SPEED_MIN_KMH, HIGH_SPEED_MAX_KMH) - HIGH_SPEED_MIN_KMH) /
                    (HIGH_SPEED_MAX_KMH - HIGH_SPEED_MIN_KMH)
            } else {
                // Normal mode maps the eight intervals from 0 to 80 km/h in 10 km/h steps.
                speedKmh.coerceIn(0f, NORMAL_SPEED_MAX_KMH) / NORMAL_SPEED_MAX_KMH
            }
            return START_ANGLE_DEGREES - progress * SWEEP_ANGLE_DEGREES
        }

        private fun pointOnDial(angleDegrees: Float, radius: Float): Point {
            val radians = angleDegrees / 180f * PI.toFloat()
            return Point(
                x = (CENTER_X + cos(radians) * radius).roundToInt(),
                y = (CENTER_Y - sin(radians) * radius).roundToInt()
            )
        }

        private fun brightness(color: Int): Int {
            return Color.red(color) + Color.green(color) + Color.blue(color)
        }
    }

    private data class Point(val x: Int, val y: Int)

    private companion object {
        private const val MATRIX_SIZE = 25
        private const val NORMAL_SPEED_MAX_KMH = 80f
        private const val HIGH_SPEED_MODE_THRESHOLD_KMH = 100f
        private const val HIGH_SPEED_MIN_KMH = 20f
        private const val HIGH_SPEED_MAX_KMH = 180f

        private const val CENTER_X = 12f
        private const val CENTER_Y = 12f
        private const val START_ANGLE_DEGREES = 180f
        private const val SWEEP_ANGLE_DEGREES = 180f
        private const val NEEDLE_START_RADIUS = 3f
        private const val NEEDLE_TIP_RADIUS = 9f

        private val DIGIT_X = intArrayOf(7, 11, 15)
        private const val DIGIT_Y = 11

        private val DIGITS = mapOf(
            '0' to arrayOf("111", "101", "101", "101", "111"),
            '1' to arrayOf("010", "110", "010", "010", "111"),
            '2' to arrayOf("111", "001", "111", "100", "111"),
            '3' to arrayOf("111", "001", "111", "001", "111"),
            '4' to arrayOf("101", "101", "111", "001", "001"),
            '5' to arrayOf("111", "100", "111", "001", "111"),
            '6' to arrayOf("111", "100", "111", "101", "111"),
            '7' to arrayOf("111", "001", "010", "010", "010"),
            '8' to arrayOf("111", "101", "111", "101", "111"),
            '9' to arrayOf("111", "101", "111", "001", "111")
        )
    }
}
