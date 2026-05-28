package com.businessdoomguy.minigamestarter.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.businessdoomguy.minigamestarter.data.db.entity.InventoryItemEntity

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY type ASC, price ASC")
    suspend fun getAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE type = :type ORDER BY price ASC")
    suspend fun getByType(type: String): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(items: List<InventoryItemEntity>)

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET isSelected = 0 WHERE type = :type")
    suspend fun clearSelected(type: String)

    @Query("UPDATE inventory_items SET isSelected = 1 WHERE id = :id")
    suspend fun select(id: String)
}
