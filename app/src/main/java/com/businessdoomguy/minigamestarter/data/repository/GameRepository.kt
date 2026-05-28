package com.businessdoomguy.minigamestarter.data.repository

import com.businessdoomguy.minigamestarter.data.db.dao.AchievementDao
import com.businessdoomguy.minigamestarter.data.db.dao.GameStatsDao
import com.businessdoomguy.minigamestarter.data.db.dao.InventoryDao
import com.businessdoomguy.minigamestarter.data.db.dao.PlayerDao
import com.businessdoomguy.minigamestarter.data.db.entity.AchievementEntity
import com.businessdoomguy.minigamestarter.data.db.entity.GameStatsEntity
import com.businessdoomguy.minigamestarter.data.db.entity.InventoryItemEntity
import com.businessdoomguy.minigamestarter.data.db.entity.PlayerProfileEntity
import com.businessdoomguy.minigamestarter.domain.model.GameResult
import com.businessdoomguy.minigamestarter.games.jumper.JumperGameResult
import kotlin.math.max
import kotlin.math.min

class GameRepository(
    private val scoreRepository: ScoreRepository,
    private val playerDao: PlayerDao,
    private val inventoryDao: InventoryDao,
    private val achievementDao: AchievementDao,
    private val gameStatsDao: GameStatsDao
) {

    suspend fun saveGameResult(result: GameResult) {
        scoreRepository.saveScore(
            gameId = result.gameId,
            score = result.score,
            durationMs = result.durationMs
        )
    }

    suspend fun saveJumperResult(result: JumperGameResult) {
        ensureDefaults()
        saveGameResult(
            GameResult(
                gameId = GAME_ID,
                score = result.score,
                durationMs = 0L,
                finishedAt = System.currentTimeMillis()
            )
        )
        if (result.rewardCoins > 0) {
            addCoins(result.rewardCoins)
        }
        if (result.passed) {
            unlockUpToLevel(result.level + 1)
        }
        updateAchievements(result)
    }

    /**
     * Marks every level up to [level] as unlocked. The value never decreases,
     * so replaying an earlier level cannot lock later progress.
     */
    suspend fun unlockUpToLevel(level: Int) {
        val profile = getProfile()
        val target = level.coerceIn(1, MAX_LEVEL)
        if (target > profile.maxUnlockedLevel) {
            playerDao.update(profile.copy(maxUnlockedLevel = target))
        }
    }

    suspend fun getMaxUnlockedLevel(): Int = getProfile().maxUnlockedLevel.coerceIn(1, MAX_LEVEL)

    suspend fun getBestScore(gameId: String): Int {
        return scoreRepository.getBestScore(gameId)
    }

    suspend fun getTopScores(limit: Int = 10) = scoreRepository.getBestScores(GAME_ID).take(limit)

    suspend fun getProfile(): PlayerProfileEntity {
        ensureDefaults()
        return requireNotNull(playerDao.getProfile())
    }

    suspend fun getCoins(): Int = getProfile().coins

    suspend fun addCoins(amount: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(coins = max(0, profile.coins + amount)))
    }

    suspend fun getShopItems(): List<InventoryItemEntity> {
        ensureDefaults()
        return inventoryDao.getAll()
    }

    suspend fun buyOrSelectItem(itemId: String): PurchaseResult {
        ensureDefaults()
        val item = inventoryDao.getById(itemId) ?: return PurchaseResult.NotFound
        val profile = getProfile()
        if (!item.isUnlocked && profile.coins < item.price) return PurchaseResult.NotEnoughCoins

        val unlockedItem = if (item.isUnlocked) item else item.copy(isUnlocked = true)
        if (!item.isUnlocked) {
            playerDao.update(profile.copy(coins = profile.coins - item.price))
            inventoryDao.update(unlockedItem)
        }
        inventoryDao.clearSelected(unlockedItem.type)
        inventoryDao.select(unlockedItem.id)
        val latestProfile = getProfile()
        playerDao.update(
            when (unlockedItem.type) {
                TYPE_SKIN -> latestProfile.copy(selectedSkinId = unlockedItem.id)
                TYPE_BACKGROUND -> latestProfile.copy(selectedBackgroundId = unlockedItem.id)
                else -> latestProfile
            }
        )
        return PurchaseResult.Success
    }

    suspend fun getAchievements(): List<AchievementEntity> {
        ensureDefaults()
        return achievementDao.getAll()
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(soundEnabled = enabled))
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(musicEnabled = enabled))
    }

    suspend fun ensureDefaults() {
        if (playerDao.getProfile() == null) {
            playerDao.insert(PlayerProfileEntity())
        }
        gameStatsDao.insert(GameStatsEntity())
        inventoryDao.insertDefaults(defaultShopItems())
        achievementDao.insertDefaults(defaultAchievements())
    }

    private suspend fun updateAchievements(result: JumperGameResult) {
        val old = gameStatsDao.getStats() ?: GameStatsEntity()
        val newPerfectStreak = if (result.passed && result.hazardHits == 0) {
            old.perfectStreak + 1
        } else if (!result.passed) {
            0
        } else {
            old.perfectStreak
        }
        val stats = old.copy(
            totalJumps = old.totalJumps + result.jumpCount,
            maxHeight = max(old.maxHeight, result.heightMeters),
            springUses = old.springUses + result.springUses,
            jetpackUses = old.jetpackUses + result.jetpackUses,
            monsterKills = old.monsterKills + result.monsterKills,
            metersInOneSession = max(old.metersInOneSession, result.heightMeters),
            noHitLevelsPassed = old.noHitLevelsPassed + if (result.passed && result.hazardHits == 0) 1 else 0,
            starsCollected = old.starsCollected + result.starsCollected,
            maxSpeedReached = old.maxSpeedReached || result.levelPattern == 5,
            perfectStreak = min(newPerfectStreak, 99)
        )
        gameStatsDao.update(stats)
        val updated = defaultAchievements().map { achievement ->
            val current = when (achievement.id) {
                "first_jump" -> stats.totalJumps
                "sky_climber" -> stats.maxHeight
                "spring_lover" -> stats.springUses
                "jetpack_pilot" -> stats.jetpackUses
                "monster_stomper" -> stats.monsterKills
                "marathon_climber" -> stats.metersInOneSession
                "untouchable" -> stats.noHitLevelsPassed
                "star_collector" -> stats.starsCollected
                "speed_demon" -> if (stats.maxSpeedReached) 1 else 0
                "jumpy_legend" -> stats.perfectStreak
                else -> 0
            }
            achievement.copy(
                currentValue = current,
                isUnlocked = current >= achievement.targetValue
            )
        }
        achievementDao.update(updated)
    }

    private fun defaultShopItems(): List<InventoryItemEntity> = listOf(
        InventoryItemEntity("skin_doodle_boy", TYPE_SKIN, "Doodle Boy", "Classic cartoon boy.", 0, true, true),
        InventoryItemEntity("skin_ninja", TYPE_SKIN, "Ninja", "Small ninja with a mask.", 200, false, false),
        InventoryItemEntity("skin_robot", TYPE_SKIN, "Robot", "Jumping bunny robot.", 300, false, false),
        InventoryItemEntity("skin_space_cat", TYPE_SKIN, "Space Cat", "Astro cat in a helmet.", 400, false, false),
        InventoryItemEntity("skin_dragon", TYPE_SKIN, "Dragon", "Tiny flying dragon.", 500, false, false),
        InventoryItemEntity("bg_doodle_paper", TYPE_BACKGROUND, "Default", "Default squared notebook paper.", 0, true, true),
        InventoryItemEntity("bg_forest", TYPE_BACKGROUND, "Forest", "Green forest with trees.", 100, false, false),
        InventoryItemEntity("bg_sky_high", TYPE_BACKGROUND, "Sky High", "Clouds floating in the sky.", 200, false, false),
        InventoryItemEntity("bg_underwater", TYPE_BACKGROUND, "Underwater", "Fish and seaweed world.", 300, false, false),
        InventoryItemEntity("bg_space", TYPE_BACKGROUND, "Space", "Stars, planets and galaxies.", 400, false, false),
        InventoryItemEntity("bg_lava", TYPE_BACKGROUND, "Lava World", "Volcanoes, lava and danger.", 500, false, false)
    )

    private fun defaultAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity("first_jump", "First Jump", "Make 10 jumps.", 0, 10, false),
        AchievementEntity("sky_climber", "Sky Climber", "Reach 500 meters.", 0, 500, false),
        AchievementEntity("spring_lover", "Spring Lover", "Activate 30 springs.", 0, 30, false),
        AchievementEntity("jetpack_pilot", "Jetpack Pilot", "Use jetpack 10 times.", 0, 10, false),
        AchievementEntity("monster_stomper", "Monster Stomper", "Kill 50 monsters.", 0, 50, false),
        AchievementEntity("marathon_climber", "Marathon Climber", "Pass 5000 meters in one session.", 0, 5000, false),
        AchievementEntity("untouchable", "Untouchable", "Pass a level without collisions.", 0, 1, false),
        AchievementEntity("star_collector", "Star Collector", "Collect 20 star bonuses.", 0, 20, false),
        AchievementEntity("speed_demon", "Speed Demon", "Reach maximum speed.", 0, 1, false),
        AchievementEntity("jumpy_legend", "JumpyDoodle Legend", "Pass 5 levels in a row without losing.", 0, 5, false)
    )

    sealed class PurchaseResult {
        data object Success : PurchaseResult()
        data object NotEnoughCoins : PurchaseResult()
        data object NotFound : PurchaseResult()
    }

    companion object {
        const val GAME_ID = "jumper"
        const val TYPE_SKIN = "skin"
        const val TYPE_BACKGROUND = "background"
        const val MAX_LEVEL = 45
    }
}
