package com.github.andreyasadchy.xtra.ui.stats

import com.github.andreyasadchy.xtra.db.ScreenTimeDao
import com.github.andreyasadchy.xtra.db.StreamWatchStatsDao
import com.github.andreyasadchy.xtra.model.stats.ScreenTime
import com.github.andreyasadchy.xtra.model.stats.StreamWatchStats
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StatsRepository @Inject constructor(
    private val screenTimeDao: ScreenTimeDao,
    private val streamWatchStatsDao: StreamWatchStatsDao
) {

    fun getScreenTimeFlow(date: String): Flow<ScreenTime?> = screenTimeDao.getScreenTimeFlow(date)

    suspend fun updateScreenTime(date: String, secondsToAdd: Long) {
        val current = screenTimeDao.getScreenTime(date)
        val newTime = (current?.totalSeconds ?: 0L) + secondsToAdd
        screenTimeDao.insert(ScreenTime(date, newTime))
    }

    fun getAllScreenTime(): Flow<List<ScreenTime>> = screenTimeDao.getAllScreenTime()

    fun getTopWatchedStreams(limit: Int): Flow<List<StreamWatchStats>> = streamWatchStatsDao.getTopWatchedStreams(limit)
    
    suspend fun updateStreamWatchStats(channelId: String, channelName: String, secondsToAdd: Long) {
        val current = streamWatchStatsDao.getStreamStats(channelId)
        val totalSeconds = (current?.totalSecondsWatched ?: 0L) + secondsToAdd
        streamWatchStatsDao.insert(StreamWatchStats(channelId, channelName, totalSeconds, System.currentTimeMillis()))
    }
}
