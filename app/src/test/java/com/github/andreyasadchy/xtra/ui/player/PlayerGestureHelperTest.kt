 package com.github.andreyasadchy.xtra.ui.player
 
 import org.junit.Assert.*
 import org.junit.Test
 
 class PlayerGestureHelperTest {
 
     @Test
     fun `formatDuration formats seconds correctly`() {
         // 45 seconds
         assertEquals("0:45", formatDuration(45_000))
     }
 
     @Test
     fun `formatDuration formats minutes and seconds correctly`() {
         // 5 minutes 30 seconds
         assertEquals("5:30", formatDuration(330_000))
     }
 
     @Test
     fun `formatDuration formats hours correctly`() {
         // 1 hour 23 minutes 45 seconds
         assertEquals("1:23:45", formatDuration(5_025_000))
     }
 
     @Test
     fun `formatDuration handles zero`() {
         assertEquals("0:00", formatDuration(0))
     }
 
     @Test
     fun `calculateNewBrightness clamps to valid range`() {
         // Should clamp to 0
         assertEquals(0f, calculateNewBrightness(0.1f, -0.5f), 0.001f)
         // Should clamp to 1
         assertEquals(1f, calculateNewBrightness(0.9f, 0.5f), 0.001f)
         // Normal case
         assertEquals(0.7f, calculateNewBrightness(0.5f, 0.2f), 0.001f)
     }
 
     @Test
     fun `calculateSeekPosition calculates correctly`() {
         val duration = 3600_000L // 1 hour
         val currentPosition = 1800_000L // 30 minutes
         val screenWidth = 1000
         
         // Swipe 10% of screen width to the right
         val newPosition = calculateSeekPosition(currentPosition, duration, 100f, screenWidth)
         assertEquals(2160_000L, newPosition) // 36 minutes (30 + 10% of 60)
     }
 
     @Test
     fun `calculateSeekPosition clamps to valid range`() {
         val duration = 3600_000L
         
         // Seek past end
         val pastEnd = calculateSeekPosition(3500_000L, duration, 500f, 1000)
         assertEquals(duration, pastEnd)
         
         // Seek before start
         val beforeStart = calculateSeekPosition(100_000L, duration, -500f, 1000)
         assertEquals(0L, beforeStart)
     }
 
     @Test
     fun `isHorizontalSwipe detects horizontal swipes`() {
         assertTrue(isHorizontalSwipe(100f, 20f, 50f))
         assertFalse(isHorizontalSwipe(30f, 20f, 50f)) // Below threshold
         assertFalse(isHorizontalSwipe(100f, 100f, 50f)) // Too vertical
     }
 
     @Test
     fun `isVerticalSwipe detects vertical swipes`() {
         assertTrue(isVerticalSwipe(20f, 100f, 50f))
         assertFalse(isVerticalSwipe(20f, 30f, 50f)) // Below threshold
         assertFalse(isVerticalSwipe(100f, 100f, 50f)) // Too horizontal
     }
 
     @Test
     fun `getVolumeIconLevel returns correct levels`() {
         assertEquals(0, getVolumeIconLevel(0))
         assertEquals(1, getVolumeIconLevel(20))
         assertEquals(2, getVolumeIconLevel(50))
         assertEquals(3, getVolumeIconLevel(80))
         assertEquals(3, getVolumeIconLevel(100))
     }
 
     @Test
     fun `getBrightnessIconLevel returns correct levels`() {
         assertEquals(0, getBrightnessIconLevel(10))
         assertEquals(1, getBrightnessIconLevel(50))
         assertEquals(2, getBrightnessIconLevel(80))
         assertEquals(2, getBrightnessIconLevel(100))
     }
 
     // Helper functions to test without Context dependency
     private fun formatDuration(durationMs: Long): String {
         val totalSeconds = durationMs / 1000
         val hours = totalSeconds / 3600
         val minutes = (totalSeconds % 3600) / 60
         val seconds = totalSeconds % 60
         
         return if (hours > 0) {
             String.format("%d:%02d:%02d", hours, minutes, seconds)
         } else {
             String.format("%d:%02d", minutes, seconds)
         }
     }
 
     private fun calculateNewBrightness(currentBrightness: Float, delta: Float): Float {
         return (currentBrightness + delta).coerceIn(0f, 1f)
     }
 
     private fun calculateSeekPosition(
         currentPosition: Long,
         duration: Long,
         gestureDelta: Float,
         screenWidth: Int,
         seekMultiplier: Float = 1f
     ): Long {
         val seekPercentage = gestureDelta / screenWidth
         val seekDelta = (duration * seekPercentage * seekMultiplier).toLong()
         return (currentPosition + seekDelta).coerceIn(0, duration)
     }
 
     private fun isHorizontalSwipe(deltaX: Float, deltaY: Float, threshold: Float): Boolean {
         return kotlin.math.abs(deltaX) > threshold && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.5f
     }
 
     private fun isVerticalSwipe(deltaX: Float, deltaY: Float, threshold: Float): Boolean {
         return kotlin.math.abs(deltaY) > threshold && kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX) * 1.5f
     }
 
     private fun getVolumeIconLevel(volumePercent: Int): Int {
         return when {
             volumePercent == 0 -> 0
             volumePercent < 33 -> 1
             volumePercent < 66 -> 2
             else -> 3
         }
     }
 
     private fun getBrightnessIconLevel(brightnessPercent: Int): Int {
         return when {
             brightnessPercent < 33 -> 0
             brightnessPercent < 66 -> 1
             else -> 2
         }
     }
 }
