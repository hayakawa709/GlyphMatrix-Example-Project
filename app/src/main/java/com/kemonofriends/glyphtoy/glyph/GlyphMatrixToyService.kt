package com.kemonofriends.glyphtoy.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

abstract class GlyphMatrixToyService(private val tag: String) : Service() {

    private val buttonPressedHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> handleGlyphToyEvent(msg)
                else -> {
                    Log.d(LOG_TAG, "Message: ${msg.what}")
                    super.handleMessage(msg)
                }
            }
        }
    }

    private val serviceMessenger = Messenger(buttonPressedHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                Log.d(LOG_TAG, "$tag: onServiceConnected")
                gmm.register(Glyph.DEVICE_23112)
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.d(LOG_TAG, "$tag: onServiceDisconnected")
            performOnServiceDisconnected(applicationContext)
            glyphMatrixManager = null
        }
    }

    final override fun onBind(intent: Intent?): IBinder {
        Log.d(LOG_TAG, "$tag: onBind")
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
            Log.d(LOG_TAG, "$tag: onBind completed")
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "$tag: onUnbind")
        glyphMatrixManager?.let {
            performOnServiceDisconnected(applicationContext)
            it.turnOff()
            it.unInit()
        }
        glyphMatrixManager = null
        return false
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}
    open fun performOnServiceDisconnected(context: Context) {}
    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}
    open fun onAod() {}

    private fun handleGlyphToyEvent(msg: Message) {
        val data = msg.data ?: return
        val event = data.getString(KEY_DATA) ?: data.getString(GlyphToy.MSG_GLYPH_TOY_DATA) ?: return

        when (event) {
            GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
            GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
            GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
            GlyphToy.EVENT_AOD -> onAod()
        }
    }

    private companion object {
        private val LOG_TAG = GlyphMatrixToyService::class.java.simpleName
        private const val KEY_DATA = "data"
    }
}
