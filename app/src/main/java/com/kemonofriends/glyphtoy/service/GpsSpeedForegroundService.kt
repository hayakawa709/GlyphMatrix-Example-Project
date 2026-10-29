package com.kemonofriends.glyphtoy.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.kemonofriends.glyphtoy.R

class GpsSpeedForegroundService : Service() {

    private val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private var isListeningForLocation = false
    private val locationListener = LocationListener { location ->
        if (location.hasSpeed()) {
            GpsSpeedStore.publish(location.speed * METERS_PER_SECOND_TO_KMH)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promoteToForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationUpdates()
            ACTION_STOP_TRACKING -> stopLocationUpdates()
        }
        // The service must only be started while the activity is visible and has location access.
        // Do not let Android recreate it later from the background after process eviction.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground(): Boolean {
        return try {
            createNotificationChannel()
            val notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.gps_service_notification_title))
                .setContentText(getString(R.string.gps_service_notification_text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            true
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to start the location foreground service", exception)
            stopSelf()
            false
        }
    }

    private fun startLocationUpdates() {
        if (isListeningForLocation || !hasFineLocationPermission()) return
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                GPS_UPDATE_INTERVAL_MS,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
            isListeningForLocation = true
        } catch (exception: SecurityException) {
            Log.w(TAG, "Location permission was revoked before GPS tracking could start", exception)
        }
    }

    private fun stopLocationUpdates() {
        if (!isListeningForLocation) return
        try {
            locationManager.removeUpdates(locationListener)
        } catch (exception: SecurityException) {
            Log.w(TAG, "Location permission was revoked before GPS tracking could stop", exception)
        }
        isListeningForLocation = false
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.gps_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START_TRACKING = "com.kemonofriends.glyphtoy.action.START_GPS_TRACKING"
        const val ACTION_STOP_TRACKING = "com.kemonofriends.glyphtoy.action.STOP_GPS_TRACKING"

        fun startTracking(context: Context) {
            context.startService(
                Intent(context, GpsSpeedForegroundService::class.java).setAction(ACTION_START_TRACKING)
            )
        }

        fun stopTracking(context: Context) {
            context.startService(
                Intent(context, GpsSpeedForegroundService::class.java).setAction(ACTION_STOP_TRACKING)
            )
        }

        private const val TAG = "GpsSpeedForegroundService"
        private const val CHANNEL_ID = "gps_speedometer"
        private const val NOTIFICATION_ID = 1001
        private const val GPS_UPDATE_INTERVAL_MS = 500L
        private const val METERS_PER_SECOND_TO_KMH = 3.6f
    }
}

object GpsSpeedStore {
    @Volatile
    private var latestSpeedKmh: Float? = null

    @Volatile
    private var listener: ((Float) -> Unit)? = null

    fun publish(speedKmh: Float) {
        latestSpeedKmh = speedKmh
        listener?.invoke(speedKmh)
    }

    fun subscribe(listener: (Float) -> Unit) {
        this.listener = listener
        latestSpeedKmh?.let(listener)
    }

    fun clear() {
        latestSpeedKmh = null
    }

    fun unsubscribe(listener: (Float) -> Unit) {
        if (this.listener === listener) {
            this.listener = null
        }
    }
}
