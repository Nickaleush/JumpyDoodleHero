package com.businessdoomguy.minigamestarter.domain.model

data class GameResult(
    val gameId: String,
    val score: Int,
    val durationMs: Long,
    val finishedAt: Long
)
