package com.businessdoomguy.minigamestarter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val currentValue: Int,
    val targetValue: Int,
    val isUnlocked: Boolean
)
