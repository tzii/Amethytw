package com.github.andreyasadchy.xtra.ui.player

import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.ui.player.ExoPlayerService.PlayerSlot

/**
 * Controls multi-stream functionality in the player.
 * 
 * Responsibilities:
 * - Managing secondary stream lifecycle
 * - Coordinating with ExoPlayerService for player management
 * - Handling audio switching between streams
 * - Managing layout transitions via MultiStreamLayoutManager
 * - Handling tap gestures for audio switching
 */
@UnstableApi
class MultiStreamController(
    private val fragment: Fragment,
    private val multiStreamContainer: ConstraintLayout,
    private val primaryAspectRatioFrame: AspectRatioFrameLayout,
    private val primarySurface: SurfaceView,
    private val secondaryContainer: FrameLayout,
    private val secondaryAspectRatioFrame: AspectRatioFrameLayout,
    private val secondarySurface: SurfaceView
) {

    // Layout manager for animated transitions
    val layoutManager: MultiStreamLayoutManager = MultiStreamLayoutManager(multiStreamContainer)

    // Service reference (set when bound)
    private var service: ExoPlayerService? = null

    // State
    private var isMultiStreamEnabled = false
    private var secondaryChannelName: String? = null
    private var secondaryStreamTitle: String? = null

    // View references
    private val primaryAudioIndicator: FrameLayout? by lazy {
        multiStreamContainer.findViewById(R.id.primaryAudioIndicator)
    }
    private val secondaryAudioIndicator: FrameLayout? by lazy {
        secondaryContainer.findViewById(R.id.secondaryAudioIndicator)
    }
    private val secondaryChannelNameView: TextView? by lazy {
        secondaryContainer.findViewById(R.id.secondaryChannelName)
    }
    private val secondaryCloseButton: View? by lazy {
        secondaryContainer.findViewById(R.id.secondaryCloseButton)
    }
    private val secondaryDragView: View? by lazy {
        secondaryContainer.findViewById(R.id.secondaryDragView)
    }
    private val secondaryBufferingIndicator: View? by lazy {
        secondaryContainer.findViewById(R.id.secondaryBufferingIndicator)
    }

    // Callbacks
    var onAudioSwitched: ((PlayerSlot) -> Unit)? = null
    var onSecondaryStreamClosed: (() -> Unit)? = null
    var onLayoutModeChanged: ((MultiStreamLayoutManager.LayoutMode) -> Unit)? = null

    init {
        setupTouchListeners()
        setupCloseButton()
    }

    /**
     * Bind to the player service
     */
    fun bindService(service: ExoPlayerService) {
        this.service = service
        updateAudioIndicators()
    }

    /**
     * Unbind from the player service
     */
    fun unbindService() {
        this.service = null
    }

    /**
     * Check if multi-stream is currently active
     */
    fun isMultiStreamActive(): Boolean = isMultiStreamEnabled

    /**
     * Add a secondary stream
     * 
     * @param mediaItem The media item for the secondary stream
     * @param channelName The channel name for display
     * @param streamTitle Optional stream title
     * @param layoutMode The initial layout mode (default: SPLIT_HORIZONTAL)
     * @return true if successfully started, false otherwise
     */
    fun addSecondaryStream(
        mediaItem: MediaItem,
        channelName: String,
        streamTitle: String? = null,
        layoutMode: MultiStreamLayoutManager.LayoutMode = MultiStreamLayoutManager.LayoutMode.SPLIT_HORIZONTAL
    ): Boolean {
        val svc = service ?: return false

        // Create secondary player
        val secondaryPlayer = svc.createSecondaryPlayer() ?: return false

        // Store stream info
        secondaryChannelName = channelName
        secondaryStreamTitle = streamTitle
        updateSecondaryStreamInfo()

        // Attach player to surface
        secondaryPlayer.setVideoSurfaceView(secondarySurface)

        // Add player listener for buffering state
        secondaryPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateSecondaryBufferingIndicator(playbackState == Player.STATE_BUFFERING)
            }
            
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    secondaryAspectRatioFrame.setAspectRatio(
                        videoSize.width.toFloat() / videoSize.height.toFloat()
                    )
                }
            }
        })

        // Load and play the media
        secondaryPlayer.setMediaItem(mediaItem)
        secondaryPlayer.prepare()
        secondaryPlayer.play()

        // Enable multi-stream mode
        isMultiStreamEnabled = true

        // Show secondary container and set layout
        layoutManager.enableMultiStream(layoutMode)

        // Update audio indicators
        updateAudioIndicators()

        // Show stream info briefly
        showSecondaryStreamInfo()

        return true
    }

    /**
     * Remove the secondary stream and return to single mode
     */
    fun removeSecondaryStream() {
        val svc = service ?: return

        // Release secondary player
        svc.releaseSecondaryPlayer()

        // Clear stream info
        secondaryChannelName = null
        secondaryStreamTitle = null

        // Return to single mode
        layoutManager.disableMultiStream()

        // Update state
        isMultiStreamEnabled = false

        // Update audio indicators
        updateAudioIndicators()

        // Notify callback
        onSecondaryStreamClosed?.invoke()
    }

    /**
     * Switch audio focus to the specified stream
     */
    fun switchAudioTo(slot: PlayerSlot) {
        service?.setAudioSource(slot)
        updateAudioIndicators()
        onAudioSwitched?.invoke(slot)
    }

    /**
     * Toggle audio between primary and secondary
     */
    fun toggleAudio(): PlayerSlot? {
        val svc = service ?: return null
        val newSlot = svc.toggleAudioSource()
        updateAudioIndicators()
        onAudioSwitched?.invoke(newSlot)
        return newSlot
    }

    /**
     * Get the current audio slot
     */
    fun getActiveAudioSlot(): PlayerSlot? = service?.getActiveAudioSlot()

    /**
     * Cycle through layout modes
     */
    fun cycleLayoutMode(): MultiStreamLayoutManager.LayoutMode {
        val newMode = layoutManager.cycleLayoutMode()
        onLayoutModeChanged?.invoke(newMode)
        return newMode
    }

    /**
     * Set a specific layout mode
     */
    fun setLayoutMode(mode: MultiStreamLayoutManager.LayoutMode) {
        layoutManager.setLayoutMode(mode)
        onLayoutModeChanged?.invoke(mode)
    }

    /**
     * Get the secondary player (for direct access if needed)
     */
    fun getSecondaryPlayer(): ExoPlayer? = service?.getSecondaryPlayer()

    // ==================== Private Methods ====================

    private fun setupTouchListeners() {
        // Primary player tap -> switch audio to primary
        multiStreamContainer.findViewById<View>(R.id.dragView)?.setOnClickListener {
            if (isMultiStreamEnabled) {
                switchAudioTo(PlayerSlot.PRIMARY)
            }
        }

        // Secondary player tap -> switch audio to secondary
        secondaryDragView?.setOnClickListener {
            if (isMultiStreamEnabled) {
                switchAudioTo(PlayerSlot.SECONDARY)
            }
        }
    }

    private fun setupCloseButton() {
        secondaryCloseButton?.setOnClickListener {
            removeSecondaryStream()
        }
    }

    private fun updateAudioIndicators() {
        val activeSlot = service?.getActiveAudioSlot() ?: PlayerSlot.PRIMARY
        val showIndicators = isMultiStreamEnabled

        primaryAudioIndicator?.visibility = if (showIndicators && activeSlot == PlayerSlot.PRIMARY) {
            View.VISIBLE
        } else {
            View.GONE
        }

        secondaryAudioIndicator?.visibility = if (showIndicators && activeSlot == PlayerSlot.SECONDARY) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateSecondaryStreamInfo() {
        secondaryChannelNameView?.text = secondaryChannelName ?: ""
    }

    private fun showSecondaryStreamInfo() {
        val infoLayout = secondaryContainer.findViewById<View>(R.id.secondaryStreamInfo)
        infoLayout?.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction {
                    postDelayed({
                        animate()
                            .alpha(0f)
                            .setDuration(200)
                            .withEndAction {
                                visibility = View.GONE
                            }
                            .start()
                    }, 3000)
                }
                .start()
        }
    }

    private fun updateSecondaryBufferingIndicator(isBuffering: Boolean) {
        secondaryBufferingIndicator?.visibility = if (isBuffering) View.VISIBLE else View.GONE
    }

    /**
     * Clean up resources
     */
    fun release() {
        if (isMultiStreamEnabled) {
            removeSecondaryStream()
        }
        service = null
    }
}
