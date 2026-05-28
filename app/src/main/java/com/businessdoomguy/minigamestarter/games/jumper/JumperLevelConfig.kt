package com.businessdoomguy.minigamestarter.games.jumper

/**
 * Level parameters for Jumper.
 *
 * There are 45 levels. The first five define five unique difficulty
 * archetypes (target height, platform types, base scroll speed). Levels 6-45
 * cycle those five archetypes, BUT every numeric difficulty value is also
 * scaled by a continuous per-level ramp, so e.g. level 11 is harder than
 * level 6 even though both reuse archetype 1.
 *
 * This mirrors the difficulty progression described in the design document:
 * higher levels scroll faster, use more platform types and apply more
 * pressure, while staying completable thanks to the physics-validated
 * generator in [JumperGameView].
 */
data class JumperLevelConfig(
    val level: Int,
    val targetHeightMeters: Int,
    val scrollSpeed: Float,
    val platformGapMeters: Float,
    val movingPlatforms: Boolean,
    val vanishingPlatforms: Boolean,
    val fragilePlatforms: Boolean,
    val rarePlatforms: Boolean,
    val monsterChance: Float,
    val spikeChance: Float,
    val boosterChance: Float
) {
    companion object {

        const val MAX_LEVEL = 45

        private val GOAL_BY_LEVEL = intArrayOf(
            100, 300, 500, 800, 1000,        // уровни 1-5
            1300, 1600, 1900, 2200, 2500,    // уровни 6-10
            2900, 3300, 3700, 4100, 4500,    // уровни 11-15
            5000, 5500, 6000, 6500, 7000,    // уровни 16-20
            7600, 8200, 8800, 9400, 10000,   // уровни 21-25
            10700, 11400, 12100, 12800, 13500, // уровни 26-30
            14300, 15100, 15900, 16700, 17500, // уровни 31-35
            18400, 19300, 20200, 21100, 22000, // уровни 36-40
            23000, 24000, 25000, 26000, 27000  // уровни 41-45
        )

        /** Base archetype before the per-level ramp is applied. */
        private fun baseForPattern(level: Int, pattern: Int): JumperLevelConfig {
            return when (pattern) {
                1 -> JumperLevelConfig(level, 100, 0.45f, 2.6f, false, false, false, false, 0.02f, 0.02f, 0.11f)
                2 -> JumperLevelConfig(level, 300, 0.65f, 3.0f, true, false, false, false, 0.04f, 0.04f, 0.10f)
                3 -> JumperLevelConfig(level, 500, 0.85f, 3.4f, true, true, false, false, 0.07f, 0.06f, 0.09f)
                4 -> JumperLevelConfig(level, 800, 1.05f, 3.8f, true, true, true, false, 0.09f, 0.08f, 0.08f)
                else -> JumperLevelConfig(level, 1000, 1.25f, 4.2f, true, true, true, true, 0.12f, 0.10f, 0.07f)
            }
        }

        /**
         * Returns the fully resolved config for [level] (1..45), including the
         * continuous difficulty ramp on top of the base archetype.
         */
        fun forLevel(level: Int): JumperLevelConfig {
            val clamped = level.coerceIn(1, MAX_LEVEL)
            val pattern = ((clamped - 1) % 5) + 1
            val base = baseForPattern(clamped, pattern)

            // ramp: 0.0 across levels 1..5, climbing toward 1.0 by level 45.
            val ramp = ((clamped - 1) / (MAX_LEVEL - 1f)).coerceIn(0f, 1f)

            return base.copy(
                targetHeightMeters = GOAL_BY_LEVEL[clamped - 1],
                scrollSpeed = base.scrollSpeed * (1f + 0.45f * ramp),
                monsterChance = (base.monsterChance + 0.05f * ramp).coerceAtMost(0.20f),
                spikeChance = (base.spikeChance + 0.04f * ramp).coerceAtMost(0.16f)
            )
        }
    }
}
