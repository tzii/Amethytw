package com.github.andreyasadchy.xtra.ui.player

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.github.andreyasadchy.xtra.R

/**
 * Manages multi-stream layout transitions with smooth animations.
 * 
 * Supports the following layout modes:
 * - SINGLE: Standard single-stream view (primary player fills container)
 * - SPLIT_HORIZONTAL: Two streams side by side (50/50)
 * - SPLIT_VERTICAL: Two streams stacked (50/50)
 * - PIP_OVERLAY: Primary stream fullscreen, secondary as small overlay
 */
class MultiStreamLayoutManager(
    private val multiStreamContainer: ConstraintLayout
) {

    enum class LayoutMode {
        SINGLE,
        SPLIT_HORIZONTAL,
        SPLIT_VERTICAL,
        PIP_OVERLAY
    }

    // Current layout mode
    private var currentMode: LayoutMode = LayoutMode.SINGLE

    // View IDs
    private val primaryPlayerId = R.id.playerLayout
    private val secondaryPlayerId = R.id.secondaryPlayerContainer

    // PiP overlay settings
    private var pipWidth = 0.3f // 30% of container width
    private var pipHeight = 0.3f // 30% of container height
    private var pipMargin = 16 // dp converted to pixels
    private var pipCorner = PipCorner.BOTTOM_END

    enum class PipCorner {
        TOP_START,
        TOP_END,
        BOTTOM_START,
        BOTTOM_END
    }

    /**
     * Get current layout mode
     */
    fun getCurrentMode(): LayoutMode = currentMode

    /**
     * Set layout mode with animation
     */
    fun setLayoutMode(mode: LayoutMode, animate: Boolean = true) {
        if (mode == currentMode) return

        currentMode = mode

        val constraintSet = ConstraintSet()
        constraintSet.clone(multiStreamContainer)

        when (mode) {
            LayoutMode.SINGLE -> applySingleMode(constraintSet)
            LayoutMode.SPLIT_HORIZONTAL -> applySplitHorizontalMode(constraintSet)
            LayoutMode.SPLIT_VERTICAL -> applySplitVerticalMode(constraintSet)
            LayoutMode.PIP_OVERLAY -> applyPipOverlayMode(constraintSet)
        }

        if (animate) {
            val transition = ChangeBounds().apply {
                duration = 300
            }
            TransitionManager.beginDelayedTransition(multiStreamContainer, transition)
        }

        constraintSet.applyTo(multiStreamContainer)

        // Update visibility
        val secondaryPlayer = multiStreamContainer.findViewById<View>(secondaryPlayerId)
        secondaryPlayer?.visibility = if (mode == LayoutMode.SINGLE) View.GONE else View.VISIBLE

        // Update close button visibility (only for PiP mode)
        val closeButton = multiStreamContainer.findViewById<View>(R.id.secondaryCloseButton)
        closeButton?.visibility = if (mode == LayoutMode.PIP_OVERLAY) View.VISIBLE else View.GONE
    }

    /**
     * Single mode: Primary player fills container, secondary hidden
     */
    private fun applySingleMode(constraintSet: ConstraintSet) {
        // Primary player fills container
        constraintSet.connect(primaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(primaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(primaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(primaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainWidth(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)

        // Clear secondary constraints
        constraintSet.clear(secondaryPlayerId)
        constraintSet.constrainWidth(secondaryPlayerId, 0)
        constraintSet.constrainHeight(secondaryPlayerId, 0)
    }

    /**
     * Split horizontal mode: Two streams side by side (50/50)
     */
    private fun applySplitHorizontalMode(constraintSet: ConstraintSet) {
        // Primary player on the left half
        constraintSet.connect(primaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(primaryPlayerId, ConstraintSet.END, secondaryPlayerId, ConstraintSet.START)
        constraintSet.connect(primaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(primaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainWidth(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setHorizontalWeight(primaryPlayerId, 1f)

        // Secondary player on the right half
        constraintSet.connect(secondaryPlayerId, ConstraintSet.START, primaryPlayerId, ConstraintSet.END)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainWidth(secondaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(secondaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setHorizontalWeight(secondaryPlayerId, 1f)

        // Create horizontal chain
        constraintSet.createHorizontalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
            ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
            intArrayOf(primaryPlayerId, secondaryPlayerId),
            null,
            ConstraintSet.CHAIN_SPREAD
        )
    }

    /**
     * Split vertical mode: Two streams stacked (50/50)
     */
    private fun applySplitVerticalMode(constraintSet: ConstraintSet) {
        // Primary player on top half
        constraintSet.connect(primaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(primaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(primaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(primaryPlayerId, ConstraintSet.BOTTOM, secondaryPlayerId, ConstraintSet.TOP)
        constraintSet.constrainWidth(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setVerticalWeight(primaryPlayerId, 1f)

        // Secondary player on bottom half
        constraintSet.connect(secondaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.TOP, primaryPlayerId, ConstraintSet.BOTTOM)
        constraintSet.connect(secondaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainWidth(secondaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(secondaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.setVerticalWeight(secondaryPlayerId, 1f)

        // Create vertical chain
        constraintSet.createVerticalChain(
            ConstraintSet.PARENT_ID, ConstraintSet.TOP,
            ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
            intArrayOf(primaryPlayerId, secondaryPlayerId),
            null,
            ConstraintSet.CHAIN_SPREAD
        )
    }

    /**
     * PiP overlay mode: Primary fullscreen, secondary as small overlay
     */
    private fun applyPipOverlayMode(constraintSet: ConstraintSet) {
        // Primary player fills container
        constraintSet.connect(primaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(primaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(primaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(primaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainWidth(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)
        constraintSet.constrainHeight(primaryPlayerId, ConstraintSet.MATCH_CONSTRAINT)

        // Secondary player as small overlay
        constraintSet.clear(secondaryPlayerId)
        constraintSet.constrainPercentWidth(secondaryPlayerId, pipWidth)
        constraintSet.constrainPercentHeight(secondaryPlayerId, pipHeight)
        constraintSet.setDimensionRatio(secondaryPlayerId, "16:9")

        // Position based on corner
        when (pipCorner) {
            PipCorner.TOP_START -> {
                constraintSet.connect(secondaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, pipMargin)
                constraintSet.connect(secondaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, pipMargin)
            }
            PipCorner.TOP_END -> {
                constraintSet.connect(secondaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, pipMargin)
                constraintSet.connect(secondaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, pipMargin)
            }
            PipCorner.BOTTOM_START -> {
                constraintSet.connect(secondaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, pipMargin)
                constraintSet.connect(secondaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, pipMargin)
            }
            PipCorner.BOTTOM_END -> {
                constraintSet.connect(secondaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, pipMargin)
                constraintSet.connect(secondaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, pipMargin)
            }
        }

        // Ensure secondary is above primary in Z-order
        multiStreamContainer.findViewById<View>(secondaryPlayerId)?.bringToFront()
    }

    /**
     * Cycle through layout modes (excluding SINGLE)
     */
    fun cycleLayoutMode(): LayoutMode {
        val nextMode = when (currentMode) {
            LayoutMode.SINGLE -> LayoutMode.SPLIT_HORIZONTAL
            LayoutMode.SPLIT_HORIZONTAL -> LayoutMode.SPLIT_VERTICAL
            LayoutMode.SPLIT_VERTICAL -> LayoutMode.PIP_OVERLAY
            LayoutMode.PIP_OVERLAY -> LayoutMode.SPLIT_HORIZONTAL
        }
        setLayoutMode(nextMode)
        return nextMode
    }

    /**
     * Set PiP overlay corner
     */
    fun setPipCorner(corner: PipCorner, animate: Boolean = true) {
        pipCorner = corner
        if (currentMode == LayoutMode.PIP_OVERLAY) {
            setLayoutMode(LayoutMode.PIP_OVERLAY, animate)
        }
    }

    /**
     * Cycle PiP corner position
     */
    fun cyclePipCorner(): PipCorner {
        pipCorner = when (pipCorner) {
            PipCorner.BOTTOM_END -> PipCorner.BOTTOM_START
            PipCorner.BOTTOM_START -> PipCorner.TOP_START
            PipCorner.TOP_START -> PipCorner.TOP_END
            PipCorner.TOP_END -> PipCorner.BOTTOM_END
        }
        if (currentMode == LayoutMode.PIP_OVERLAY) {
            setLayoutMode(LayoutMode.PIP_OVERLAY)
        }
        return pipCorner
    }

    /**
     * Set PiP overlay size
     */
    fun setPipSize(widthPercent: Float, heightPercent: Float) {
        pipWidth = widthPercent.coerceIn(0.2f, 0.5f)
        pipHeight = heightPercent.coerceIn(0.2f, 0.5f)
        if (currentMode == LayoutMode.PIP_OVERLAY) {
            setLayoutMode(LayoutMode.PIP_OVERLAY)
        }
    }

    /**
     * Set PiP margin in pixels
     */
    fun setPipMargin(marginPx: Int) {
        pipMargin = marginPx
        if (currentMode == LayoutMode.PIP_OVERLAY) {
            setLayoutMode(LayoutMode.PIP_OVERLAY)
        }
    }

    /**
     * Swap primary and secondary stream positions
     */
    fun swapStreams(animate: Boolean = true) {
        val primaryView = multiStreamContainer.findViewById<ViewGroup>(primaryPlayerId)
        val secondaryView = multiStreamContainer.findViewById<ViewGroup>(secondaryPlayerId)

        if (primaryView == null || secondaryView == null) return

        // For split modes, we can visually swap by swapping constraints
        when (currentMode) {
            LayoutMode.SPLIT_HORIZONTAL, LayoutMode.SPLIT_VERTICAL -> {
                // Create reverse constraint set
                val constraintSet = ConstraintSet()
                constraintSet.clone(multiStreamContainer)

                if (currentMode == LayoutMode.SPLIT_HORIZONTAL) {
                    // Swap left/right
                    constraintSet.connect(primaryPlayerId, ConstraintSet.START, secondaryPlayerId, ConstraintSet.END)
                    constraintSet.connect(primaryPlayerId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                    constraintSet.connect(secondaryPlayerId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                    constraintSet.connect(secondaryPlayerId, ConstraintSet.END, primaryPlayerId, ConstraintSet.START)
                } else {
                    // Swap top/bottom
                    constraintSet.connect(primaryPlayerId, ConstraintSet.TOP, secondaryPlayerId, ConstraintSet.BOTTOM)
                    constraintSet.connect(primaryPlayerId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                    constraintSet.connect(secondaryPlayerId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    constraintSet.connect(secondaryPlayerId, ConstraintSet.BOTTOM, primaryPlayerId, ConstraintSet.TOP)
                }

                if (animate) {
                    val transition = ChangeBounds().apply {
                        duration = 300
                    }
                    TransitionManager.beginDelayedTransition(multiStreamContainer, transition)
                }

                constraintSet.applyTo(multiStreamContainer)
            }
            LayoutMode.PIP_OVERLAY -> {
                // Swap which player is fullscreen vs PiP by toggling constraints
                // This is handled at a higher level by swapping player content
            }
            LayoutMode.SINGLE -> {
                // No swap possible in single mode
            }
        }
    }

    /**
     * Check if multi-stream mode is active
     */
    fun isMultiStreamActive(): Boolean = currentMode != LayoutMode.SINGLE

    /**
     * Enable multi-stream mode with specified layout
     */
    fun enableMultiStream(mode: LayoutMode = LayoutMode.SPLIT_HORIZONTAL) {
        if (mode == LayoutMode.SINGLE) return
        setLayoutMode(mode)
    }

    /**
     * Disable multi-stream mode (return to single)
     */
    fun disableMultiStream() {
        setLayoutMode(LayoutMode.SINGLE)
    }
}
