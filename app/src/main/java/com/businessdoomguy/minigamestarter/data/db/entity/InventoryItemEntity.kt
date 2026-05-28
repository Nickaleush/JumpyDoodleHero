package com.businessdoomguy.minigamestarter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String,
    val price: Int,
    val isUnlocked: Boolean,
    val isSelected: Boolean
)
