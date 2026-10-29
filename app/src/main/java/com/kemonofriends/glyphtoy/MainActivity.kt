package com.kemonofriends.glyphtoy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kemonofriends.glyphtoy.service.GpsSpeedForegroundService
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kemonofriends.glyphtoy.ui.theme.KemonoFriendsGlyphToyTheme

class MainActivity : ComponentActivity() {
    private val requestLocationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startGpsForegroundService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFineLocationPermissionIfNeeded()
        if (hasFineLocationPermission()) {
            startGpsForegroundService()
        }
        enableEdgeToEdge()
        setContent {
            KemonoFriendsGlyphToyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun requestFineLocationPermissionIfNeeded() {
        if (!hasFineLocationPermission()) {
            requestLocationPermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startGpsForegroundService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, GpsSpeedForegroundService::class.java)
        )
    }

}

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    Text(
        text = "Kemono Friends Logo Glyph Toy",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun AppContentPreview() {
    KemonoFriendsGlyphToyTheme {
        AppContent()
    }
}
