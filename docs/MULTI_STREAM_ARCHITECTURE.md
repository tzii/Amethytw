# Multi-Stream Feature Architecture

## Overview

This document outlines the architecture for implementing dual-stream viewing in ThystTV, allowing users to watch 2 Twitch streams simultaneously (similar to SmartTwitchTV).

## Research Summary

### SmartTwitchTV Reference Implementation

SmartTwitchTV (`fgl27/SmartTwitchTV`) implements multi-stream with:
- **Multiple ExoPlayer instances** (up to 5) managed in `PlayerActivity.java`
- **Array-based management**: `MultiStreamPlayerViewLayout` for layout positions
- **Audio switching**: `AudioEnabled` array + `ApplyAudioAll()` to mute/unmute players
- **Grid layouts**: Different view modes for 2/3/4 stream arrangements
- **Hybrid architecture**: Android Native (Java) for video, JavaScript/WebView for UI

### ExoPlayer Multi-Instance Constraints

1. **Hardware decoder limits**: 2-8 simultaneous decoders (device dependent)
2. **Audio focus**: Android grants focus to app, not individual players - manual control needed
3. **Memory**: Each ExoPlayer instance consumes significant resources
4. **Best practice**: Pool players, pause inactive ones, use software decoders as fallback

## Current ThystTV Architecture

```
MainActivity
    └── PlayerFragment (abstract)
            ├── ExoPlayerFragment (Media3/ExoPlayer)
            ├── Media3Fragment (Media3 Session)
            └── MediaPlayerFragment (Android MediaPlayer)
                    │
                    └── ExoPlayerService (foreground service)
                            └── ExoPlayer instance (single)
```

**Key Files:**
- `ui/player/PlayerFragment.kt` - Base class, UI/gestures/chat
- `ui/player/ExoPlayerFragment.kt` - ExoPlayer integration
- `ui/player/ExoPlayerService.kt` - Foreground service with player
- `res/layout/fragment_player.xml` - PlayerLayout + ChatLayout

## Recommended Architecture

### 1. Service Architecture: Primary/Secondary Slot Pattern

Modify `ExoPlayerService` to hold two player slots:

```kotlin
class ExoPlayerService : Service() {
    // Existing (backward compatible)
    val player: ExoPlayer? get() = primaryPlayer
    
    // New dual-stream support
    var primaryPlayer: ExoPlayer? = null
    var secondaryPlayer: ExoPlayer? = null
    
    // Audio focus management
    private var activeAudioSlot: PlayerSlot = PlayerSlot.PRIMARY
    
    enum class PlayerSlot { PRIMARY, SECONDARY }
    
    fun setAudioSource(slot: PlayerSlot) {
        activeAudioSlot = slot
        primaryPlayer?.volume = if (slot == PlayerSlot.PRIMARY) 1.0f else 0.0f
        secondaryPlayer?.volume = if (slot == PlayerSlot.SECONDARY) 1.0f else 0.0f
    }
    
    fun createSecondaryPlayer(): ExoPlayer { ... }
    fun releaseSecondaryPlayer() { ... }
}
```

### 2. Layout Strategy: ConstraintLayout with ConstraintSet Animations

**Single Stream Mode:**
```
┌─────────────────────────────────────┐
│                                     │
│         Primary Player              │
│         (Full Screen)               │
│                                     │
├─────────────────────────────────────┤
│              Chat                   │
└─────────────────────────────────────┘
```

**Dual Stream Mode (Side-by-Side):**
```
┌─────────────────┬───────────────────┐
│                 │                   │
│    Primary      │    Secondary      │
│    Player       │    Player         │
│                 │                   │
├─────────────────┴───────────────────┤
│         Chat (toggleable)           │
└─────────────────────────────────────┘
```

**Dual Stream Mode (PiP Overlay):**
```
┌─────────────────────────────────────┐
│                              ┌─────┐│
│         Primary Player       │ Sec ││
│                              │     ││
│                              └─────┘│
├─────────────────────────────────────┤
│              Chat                   │
└─────────────────────────────────────┘
```

### 3. Component Extraction

Extract video rendering into reusable component:

```kotlin
class StreamSurfaceView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    
    private val surfaceView: SurfaceView
    private val bufferingIndicator: CircularProgressIndicator
    private val audioIndicator: ImageView  // Shows speaker/mute icon
    
    var isAudioActive: Boolean = false
        set(value) {
            field = value
            updateAudioIndicator()
        }
    
    fun attachPlayer(player: ExoPlayer) { ... }
    fun detachPlayer() { ... }
}
```

### 4. PlayerFragment Modifications

Add multi-stream state management:

```kotlin
abstract class PlayerFragment : BaseNetworkFragment() {
    // Existing
    protected var videoType: String? = null
    
    // New multi-stream support
    protected var isMultiStreamMode: Boolean = false
    protected var multiStreamLayout: MultiStreamLayout = MultiStreamLayout.SINGLE
    
    enum class MultiStreamLayout {
        SINGLE,           // One stream
        SPLIT_HORIZONTAL, // Side by side
        SPLIT_VERTICAL,   // Top/bottom
        PIP_OVERLAY       // Primary fullscreen, secondary overlay
    }
    
    fun addSecondaryStream(stream: Stream) { ... }
    fun removeSecondaryStream() { ... }
    fun toggleAudioFocus() { ... }
    fun cycleLayout() { ... }
}
```

### 5. Audio Management

```kotlin
// In ExoPlayerService
fun setAudioSource(slot: PlayerSlot) {
    activeAudioSlot = slot
    when (slot) {
        PlayerSlot.PRIMARY -> {
            primaryPlayer?.volume = prefs.getInt(C.PLAYER_VOLUME, 100) / 100f
            secondaryPlayer?.volume = 0f
        }
        PlayerSlot.SECONDARY -> {
            primaryPlayer?.volume = 0f
            secondaryPlayer?.volume = prefs.getInt(C.PLAYER_VOLUME, 100) / 100f
        }
    }
    // Update MediaSession to reflect active stream
    updateMediaSessionMetadata()
}
```

### 6. PiP and Background Handling

```kotlin
override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    if (isInPictureInPictureMode) {
        // Hide secondary, show only primary (audio source)
        secondarySurfaceView.gone()
        primarySurfaceView.fillParent()
        chatLayout.gone()
        
        // Pause secondary to save resources
        playbackService?.secondaryPlayer?.pause()
    } else {
        // Restore dual-stream layout
        restoreMultiStreamLayout()
        playbackService?.secondaryPlayer?.play()
    }
}
```

## Implementation Plan

### Phase 1: Service Refactoring
1. Add `secondaryPlayer` slot to `ExoPlayerService`
2. Implement `createSecondaryPlayer()` / `releaseSecondaryPlayer()`
3. Add `setAudioSource()` for audio switching
4. Update MediaSession to handle active stream

### Phase 2: Layout Components
1. Create `StreamSurfaceView` component
2. Add multi-stream containers to `fragment_player.xml`
3. Create `ConstraintSet` definitions for layouts
4. Implement layout transition animations

### Phase 3: PlayerFragment Integration
1. Add multi-stream state management
2. Implement `addSecondaryStream()` / `removeSecondaryStream()`
3. Add gesture handling for layout switching
4. Update controls overlay for dual-stream

### Phase 4: UI/UX Polish
1. Add "Add Stream" button to player controls
2. Stream picker dialog
3. Audio toggle indicator
4. Layout cycle button
5. Settings for default layout preference

### Phase 5: Edge Cases
1. PiP handling
2. Background playback
3. Device rotation
4. Hardware decoder fallback
5. Memory pressure handling

## Files to Modify

| File | Changes |
|------|---------|
| `ExoPlayerService.kt` | Add secondary player slot, audio management |
| `PlayerFragment.kt` | Add multi-stream layout management |
| `ExoPlayerFragment.kt` | Connect secondary player, handle lifecycle |
| `fragment_player.xml` | Add secondary player container |
| `player_layout.xml` | Add multi-stream controls |
| `PlayerSettingsDialog.kt` | Add "Add Stream" option |
| `C.kt` (constants) | Add multi-stream preferences |

## New Files to Create

| File | Purpose |
|------|---------|
| `ui/view/StreamSurfaceView.kt` | Reusable video surface component |
| `res/layout/view_stream_surface.xml` | StreamSurfaceView layout |
| `res/xml/constraint_set_single.xml` | Single stream constraints |
| `res/xml/constraint_set_split.xml` | Split view constraints |
| `res/xml/constraint_set_pip.xml` | PiP overlay constraints |
| `ui/player/StreamPickerDialog.kt` | Dialog to select second stream |

## Risk Considerations

1. **Hardware Limits**: Most devices handle 2x 1080p, but some may fail. Implement graceful fallback.
2. **Memory**: Two players double memory usage. Monitor and release secondary when not needed.
3. **Battery**: Dual decode increases power consumption significantly.
4. **Complexity**: Increases codebase complexity - ensure good test coverage.

## Testing Checklist

- [ ] Single stream mode unchanged
- [ ] Add secondary stream works
- [ ] Remove secondary stream works
- [ ] Audio toggle between streams
- [ ] Layout cycling (split/pip)
- [ ] PiP mode with dual stream
- [ ] Background playback
- [ ] Device rotation
- [ ] App minimize/restore
- [ ] Low memory handling
- [ ] Hardware decoder failure fallback
