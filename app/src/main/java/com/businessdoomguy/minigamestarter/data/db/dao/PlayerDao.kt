package com.businessdoomguy.minigamestarter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.businessdoomguy.minigamestarter.data.db.entity.PlayerProfileEntity

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: PlayerProfileEntity)

    @Update
    suspend fun update(profile: PlayerProfileEntity)
}
