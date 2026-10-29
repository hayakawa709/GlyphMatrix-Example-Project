package com.kemonofriends.glyphtoy.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.ContextCompat
import com.kemonofriends.glyphtoy.R
import com.kemonofriends.glyphtoy.glyph.GlyphMatrixToyService
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils

class KemonoFriendsLogoGlyphToyService : GlyphMatrixToyService("KemonoFriendsLogoGlyphToyService") {

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        showLogo(glyphMatrixManager)
    }

    override fun performOnServiceDisconnected(context: Context) {
        Log.d(TAG, "Glyph Matrix service disconnected")
    }

    override fun onAod() {
        glyphMatrixManager?.let { manager ->
            showLogo(manager)
        }
    }

    override fun onTouchPointPressed() {
        glyphMatrixManager?.let(::showLogo)
    }

    private fun showLogo(glyphMatrixManager: GlyphMatrixManager) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.kemono_friends_logo) ?: return
        val bitmap = GlyphMatrixUtils.drawableToBitmap(drawable)
        showBitmap(glyphMatrixManager, bitmap)
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
    }
}

