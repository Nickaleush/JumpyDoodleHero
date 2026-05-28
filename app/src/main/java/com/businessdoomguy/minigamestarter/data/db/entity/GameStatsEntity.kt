package com.businessdoomguy.minigamestarter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalJumps: Int = 0,
    val maxHeight: Int = 0,
    val springUses: Int = 0,
    val jetpackUses: Int = 0,
    val monsterKills: Int = 0,
    val metersInOneSession: Int = 0,
    val noHitLevelsPassed: Int = 0,
    val starsCollected: Int = 0,
    val maxSpeedReached: Boolean = false,
    val perfectStreak: Int = 0
)
