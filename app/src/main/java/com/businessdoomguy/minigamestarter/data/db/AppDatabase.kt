package com.businessdoomguy.minigamestarter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.businessdoomguy.minigamestarter.data.db.dao.AchievementDao
import com.businessdoomguy.minigamestarter.data.db.dao.GameStatsDao
import com.businessdoomguy.minigamestarter.data.db.dao.InventoryDao
import com.businessdoomguy.minigamestarter.data.db.dao.PlayerDao
import com.businessdoomguy.minigamestarter.data.db.dao.ScoreDao
import com.businessdoomguy.minigamestarter.data.db.entity.AchievementEntity
import com.businessdoomguy.minigamestarter.data.db.entity.GameStatsEntity
import com.businessdoomguy.minigamestarter.data.db.entity.InventoryItemEntity
import com.businessdoomguy.minigamestarter.data.db.entity.PlayerProfileEntity
import com.businessdoomguy.minigamestarter.data.db.entity.ScoreEntity

@Database(
    entities = [
        ScoreEntity::class,
        PlayerProfileEntity::class,
        InventoryItemEntity::class,
        AchievementEntity::class,
        GameStatsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scoreDao(): ScoreDao
    abstract fun playerDao(): PlayerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {

        /**
         * Adds the level-unlock progress column introduced together with the
         * level unlock feature. Using an explicit migration instead of a
         * destructive fallback keeps the player's coins, scores and inventory
         * after an app update.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profile " +
                        "ADD COLUMN maxUnlockedLevel INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "mini_game_starter.db"
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
