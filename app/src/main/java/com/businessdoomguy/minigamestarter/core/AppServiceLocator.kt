package com.businessdoomguy.minigamestarter.core

import android.content.Context
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.core.dispatchers.AppDispatchers
import com.businessdoomguy.minigamestarter.data.db.AppDatabase
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import com.businessdoomguy.minigamestarter.data.repository.ScoreRepository

class AppServiceLocator(
    private val context: Context
) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val soundManager: SoundManager by lazy {
        SoundManager(context.applicationContext)
    }

    val scoreRepository: ScoreRepository by lazy {
        ScoreRepository(database.scoreDao())
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(
            scoreRepository = scoreRepository,
            playerDao = database.playerDao(),
            inventoryDao = database.inventoryDao(),
            achievementDao = database.achievementDao(),
            gameStatsDao = database.gameStatsDao()
        )
    }

    val dispatchers: AppDispatchers by lazy {
        AppDispatchers()
    }
}
