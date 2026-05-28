package com.businessdoomguy.minigamestarter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.businessdoomguy.minigamestarter.data.db.entity.ScoreEntity

@Dao
interface ScoreDao {

    @Insert
    suspend fun insert(score: ScoreEntity)

    @Query("""
        SELECT * FROM scores
        WHERE gameId = :gameId
        ORDER BY score DESC, createdAt ASC
        LIMIT :limit
    """)
    suspend fun getBestScores(gameId: String, limit: Int = 10): List<ScoreEntity>

    @Query("SELECT MAX(score) FROM scores WHERE gameId = :gameId")
    suspend fun getBestScore(gameId: String): Int?
}
