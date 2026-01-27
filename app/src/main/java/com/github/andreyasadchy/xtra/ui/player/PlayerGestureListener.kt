package com.github.andreyasadchy.xtra.ui.player

import android.content.Context
import android.media.AudioManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.util.gone
import com.github.andreyasadchy.xtra.util.visible

interface PlayerGestureCallback {
    val isPortrait: Boolean
    val isMaximized: Boolean
    val isControlsVisible: Boolean
    val screenWidth: Int
    val screenHeight: Int
    val windowAttributes: android.view.WindowManager.LayoutParams
    
    fun setWindowAttributes(params: android.view.WindowManager.LayoutParams)
    fun showController()
    fun hideController()
    fun updateProgress()
    fun cycleChatMode()
    fun getGestureFeedbackView(): View
    fun getHideGestureRunnable(): Runnable
    fun isControllerHideOnTouch(): Boolean
}

class PlayerGestureListener(
    private val context: Context,
    private val callback: PlayerGestureCallback,
    private val doubleTapEnabled: Boolean
) : GestureDetector.SimpleOnGestureListener() {

    private var isVolume = false
    private var isBrightness = false
    private var startVolume = 0
    private var startBrightness = 0f
    private var gestureStartY = 0f

    override fun onDown(e: MotionEvent): Boolean {
        isVolume = false
        isBrightness = false
        gestureStartY = e.y
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (e1 == null || callback.isPortrait || !callback.isMaximized || callback.isControlsVisible) return false
        
        val width = callback.screenWidth.toFloat()
        val height = callback.screenHeight.toFloat()
        
        if (!isVolume && !isBrightness) {
             if (Math.abs(distanceY) > Math.abs(distanceX)) {
                 if (e1.x < width / 2) {
                     isBrightness = true
                     startBrightness = callback.windowAttributes.screenBrightness
                     if (startBrightness < 0) startBrightness = 0.5f // Default fallback
                 } else {
                     isVolume = true
                     val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                     startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                 }
             }
        }

        val percent = (gestureStartY - e2.y) / height
        val feedback = callback.getGestureFeedbackView()
        val icon = feedback.findViewById<ImageView>(R.id.volumeMute)
        val slider = feedback.findViewById<Slider>(R.id.volumeBar)
        val text = feedback.findViewById<TextView>(R.id.volumeText)

        if (isBrightness) {
            val newBrightness = (startBrightness + percent).coerceIn(0.01f, 1.0f)
            val lp = callback.windowAttributes
            lp.screenBrightness = newBrightness
            callback.setWindowAttributes(lp)
            
            icon.setImageResource(R.drawable.ic_brightness_medium_black_24dp)
            feedback.visible()
            feedback.removeCallbacks(callback.getHideGestureRunnable())
            feedback.postDelayed(callback.getHideGestureRunnable(), 1000)
            
            slider.value = newBrightness * 100
            text.text = "%d".format((newBrightness * 100).toInt())
            return true
        }
        
        if (isVolume) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (startVolume + (percent * maxVolume)).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            
            icon.setImageResource(R.drawable.baseline_volume_up_black_24)
            feedback.visible()
            feedback.removeCallbacks(callback.getHideGestureRunnable())
            feedback.postDelayed(callback.getHideGestureRunnable(), 1000)
            
            slider.value = (newVolume.toFloat() / maxVolume.toFloat()) * 100
            text.text = "%d".format((slider.value).toInt())
            return true
        }

        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return if (!doubleTapEnabled || callback.isPortrait) {
            handleSingleTap()
            true
        } else {
            false
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return if (doubleTapEnabled && !callback.isPortrait) {
            handleSingleTap()
            true
        } else {
            false
        }
    }

    private fun handleSingleTap() {
        val visible = callback.isControlsVisible
        if (visible) {
            if (callback.isControllerHideOnTouch()) {
                callback.hideController()
            }
        } else {
            callback.showController()
        }
        if (!visible) {
            callback.updateProgress()
        }
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return if (doubleTapEnabled && !callback.isPortrait && callback.isMaximized) {
            callback.cycleChatMode()
            true
        } else {
            false
        }
    }
}
