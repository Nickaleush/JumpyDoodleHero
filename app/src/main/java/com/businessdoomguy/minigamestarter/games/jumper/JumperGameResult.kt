package com.businessdoomguy.minigamestarter.games.jumper

data class JumperGameResult(
    val level: Int,
    val passed: Boolean,
    val score: Int,
    val heightMeters: Int,
    val rewardCoins: Int,
    val jumpCount: Int,
    val springUses: Int,
    val jetpackUses: Int,
    val monsterKills: Int,
    val starsCollected: Int,
    val hazardHits: Int,
    val levelPattern: Int
)
