package com.businessdoomguy.minigamestarter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 0,
    val selectedSkinId: String = "skin_doodle_boy",
    val selectedBackgroundId: String = "bg_doodle_paper",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val maxUnlockedLevel: Int = 1
)
