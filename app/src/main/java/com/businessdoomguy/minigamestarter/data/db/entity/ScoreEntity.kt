package com.businessdoomguy.minigamestarter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val gameId: String,
    val score: Int,
    val durationMs: Long,
    val createdAt: Long
)
