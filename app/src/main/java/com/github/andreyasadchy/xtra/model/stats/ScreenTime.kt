package com.github.andreyasadchy.xtra.model.stats
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "screen_time")
data class ScreenTime(@PrimaryKey val date: String, val totalSeconds: Long)
