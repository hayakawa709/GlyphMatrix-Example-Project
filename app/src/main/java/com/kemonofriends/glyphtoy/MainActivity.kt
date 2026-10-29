package com.kemonofriends.glyphtoy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kemonofriends.glyphtoy.ui.theme.KemonoFriendsGlyphToyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KemonoFriendsGlyphToyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
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
