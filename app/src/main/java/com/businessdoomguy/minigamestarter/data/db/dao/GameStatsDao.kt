package com.businessdoomguy.minigamestarter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.businessdoomguy.minigamestarter.data.db.entity.GameStatsEntity

@Dao
interface GameStatsDao {
    @Query("SELECT * FROM game_stats WHERE id = 1")
    suspend fun getStats(): GameStatsEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stats: GameStatsEntity)

    @Update
    suspend fun update(stats: GameStatsEntity)
}
