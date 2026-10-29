package com.kemonofriends.glyphtoy.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.kemonofriends.glyphtoy.R
import com.kemonofriends.glyphtoy.glyph.GlyphMatrixToyService
import com.kemonofriends.glyphtoy.speedometer.SpeedometerFrameRenderer
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils

class KemonoFriendsLogoGlyphToyService : GlyphMatrixToyService("KemonoFriendsLogoGlyphToyService") {

    private val speedometerRenderer by lazy { SpeedometerFrameRenderer(this) }
    private var isSpeedometerMode = false
    private var nextSpeedSource = SpeedSource.SIMULATED
    private var activeSpeedSource: SpeedSource? = null
    private val speedSimulationHandler = Handler(Looper.getMainLooper())
    private var isSpeedSimulationRunning = false
    private var speedSimulationStartMs = 0L
    private val speedSimulationRunnable = object : Runnable {
        override fun run() {
            if (!isSpeedometerMode || !isSpeedSimulationRunning) return

            val elapsedMs = SystemClock.elapsedRealtime() - speedSimulationStartMs
            val cycleProgress = (elapsedMs % SPEED_SIMULATION_CYCLE_MS).toFloat() /
                SPEED_SIMULATION_CYCLE_MS
            glyphMatrixManager?.let { manager ->
                showSpeedometer(manager, cycleProgress * SPEED_SIMULATION_MAX_KMH)
            }
            speedSimulationHandler.postDelayed(this, SPEED_SIMULATION_FRAME_INTERVAL_MS)
        }
    }
    private val gpsSpeedListener: (Float) -> Unit = { speedKmh ->
        if (isSpeedometerMode && activeSpeedSource == SpeedSource.GPS) {
            glyphMatrixManager?.let { manager ->
                showSpeedometer(manager, speedKmh)
            }
        }
    }

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        showLogo(glyphMatrixManager)
    }

    override fun performOnServiceDisconnected(context: Context) {
        stopSpeedSimulation()
        stopGpsSpeedUpdates()
        activeSpeedSource = null
        isSpeedometerMode = false
        Log.d(TAG, "Glyph Matrix service disconnected")
    }

    override fun onAod() {
        glyphMatrixManager?.let { manager ->
            if (isSpeedometerMode) {
                showSpeedometerPlaceholder(manager)
            } else {
                showLogo(manager)
            }
        }
    }

    override fun onTouchPointPressed() {
        glyphMatrixManager?.let { manager ->
            if (!isSpeedometerMode) {
                showLogo(manager)
            }
        }
    }

    override fun onTouchPointLongPress() {
        if (isSpeedometerMode) {
            showLogoMode()
        } else {
            showSpeedometerMode()
        }
    }

    override fun onTouchPointReleased() {
        // A long press toggles the active mode. Releasing the Glyph button must not change it.
    }

    private fun showSpeedometerMode() {
        isSpeedometerMode = true
        activeSpeedSource = nextSpeedSource
        nextSpeedSource = if (nextSpeedSource == SpeedSource.SIMULATED) {
            SpeedSource.GPS
        } else {
            SpeedSource.SIMULATED
        }
        Log.d(TAG, "Speedometer source: $activeSpeedSource")
        glyphMatrixManager?.let(::showSpeedometerPlaceholder)
        when (activeSpeedSource) {
            SpeedSource.SIMULATED -> startSpeedSimulation()
            SpeedSource.GPS -> startGpsSpeedUpdates()
            null -> Unit
        }
    }

    private fun showLogoMode() {
        isSpeedometerMode = false
        stopSpeedSimulation()
        stopGpsSpeedUpdates()
        activeSpeedSource = null
        glyphMatrixManager?.let(::showLogo)
    }

    private fun showLogo(glyphMatrixManager: GlyphMatrixManager) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.kemono_friends_logo) ?: return
        val bitmap = GlyphMatrixUtils.drawableToBitmap(drawable)
        showBitmap(glyphMatrixManager, bitmap)
    }

    private fun showSpeedometerPlaceholder(glyphMatrixManager: GlyphMatrixManager) {
        val bitmap = speedometerRenderer.renderAllOnPlaceholder()
        showBitmap(glyphMatrixManager, bitmap)
    }

    private fun showSpeedometer(glyphMatrixManager: GlyphMatrixManager, speedKmh: Float) {
        val bitmap = speedometerRenderer.render(speedKmh)
        showBitmap(glyphMatrixManager, bitmap)
    }

    private fun startSpeedSimulation() {
        if (isSpeedSimulationRunning) return
        isSpeedSimulationRunning = true
        speedSimulationStartMs = SystemClock.elapsedRealtime()
        speedSimulationHandler.post(speedSimulationRunnable)
    }

    private fun stopSpeedSimulation() {
        isSpeedSimulationRunning = false
        speedSimulationHandler.removeCallbacks(speedSimulationRunnable)
    }

    private fun startGpsSpeedUpdates() {
        if (!hasFineLocationPermission()) return
        GpsSpeedStore.clear()
        GpsSpeedStore.subscribe(gpsSpeedListener)
        GpsSpeedForegroundService.startTracking(this)
    }

    private fun stopGpsSpeedUpdates() {
        GpsSpeedStore.unsubscribe(gpsSpeedListener)
        GpsSpeedForegroundService.stopTracking(this)
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun showBitmap(glyphMatrixManager: GlyphMatrixManager, bitmap: Bitmap) {
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        glyphMatrixManager.setMatrixFrame(frame)
    }

    private companion object {
        private const val TAG = "KemonoFriendsLogoToy"
        private const val SPEED_SIMULATION_CYCLE_MS = 10_000L
        private const val SPEED_SIMULATION_FRAME_INTERVAL_MS = 100L
        private const val SPEED_SIMULATION_MAX_KMH = 180f
    }

    private enum class SpeedSource {
        SIMULATED,
        GPS
    }
}

