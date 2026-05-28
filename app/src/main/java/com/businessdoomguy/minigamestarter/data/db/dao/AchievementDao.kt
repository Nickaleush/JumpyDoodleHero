package com.businessdoomguy.minigamestarter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.businessdoomguy.minigamestarter.data.db.entity.AchievementEntity

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, title ASC")
    suspend fun getAll(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(items: List<AchievementEntity>)

    @Update
    suspend fun update(items: List<AchievementEntity>)
}
