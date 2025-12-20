package com.github.andreyasadchy.xtra.model.stats
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "stream_watch_stats")
data class StreamWatchStats(@PrimaryKey val channelId: String, val channelName: String, val totalSecondsWatched: Long, val lastWatchedTimestamp: Long)
