package com.businessdoomguy.minigamestarter.data.repository

import com.businessdoomguy.minigamestarter.data.db.dao.ScoreDao
import com.businessdoomguy.minigamestarter.data.db.entity.ScoreEntity

class ScoreRepository(
    private val scoreDao: ScoreDao
) {

    suspend fun saveScore(gameId: String, score: Int, durationMs: Long) {
        scoreDao.insert(
            ScoreEntity(
                gameId = gameId,
                score = score,
                durationMs = durationMs,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getBestScore(gameId: String): Int {
        return scoreDao.getBestScore(gameId) ?: 0
    }

    suspend fun getBestScores(gameId: String): List<ScoreEntity> {
        return scoreDao.getBestScores(gameId)
    }
}
