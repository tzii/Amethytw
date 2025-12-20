package com.github.andreyasadchy.xtra.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andreyasadchy.xtra.model.stats.StreamWatchStats
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamWatchStatsDao {
    @Query("SELECT * FROM stream_watch_stats ORDER BY totalSecondsWatched DESC LIMIT :limit")
    fun getTopWatchedStreams(limit: Int): Flow<List<StreamWatchStats>>

    @Query("SELECT * FROM stream_watch_stats WHERE channelId = :channelId")
    suspend fun getStreamStats(channelId: String): StreamWatchStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: StreamWatchStats)
}
