package com.github.andreyasadchy.xtra.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.andreyasadchy.xtra.model.stats.ScreenTime
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeDao {
    @Query("SELECT * FROM screen_time WHERE date = :date")
    fun getScreenTimeFlow(date: String): Flow<ScreenTime?>

    @Query("SELECT * FROM screen_time WHERE date = :date")
    suspend fun getScreenTime(date: String): ScreenTime?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screenTime: ScreenTime)
    
    @Query("SELECT * FROM screen_time")
    fun getAllScreenTime(): Flow<List<ScreenTime>>
}
