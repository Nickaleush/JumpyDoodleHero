package com.businessdoomguy.minigamestarter.games.jumper

import android.graphics.Color

/**
 * Centralised visual theme for the Jumper gameplay screen.
 *
 * ---------------------------------------------------------------------------
 * DESIGN ENTRY POINT — "полная смена дизайна"
 * ---------------------------------------------------------------------------
 * Every colour used while playing — background gradient, the five platform
 * types, both hazard types, boosters and the goal line — is defined here.
 * To completely re-skin the game, edit (or add) a [JumperTheme] entry below.
 * [JumperGameView] reads colours exclusively through the resolved theme, so
 * no drawing code has to be touched.
 *
 * Backgrounds shown in the shop map 1:1 onto a [JumperTheme] via [byId].
 */
data class JumperTheme(
    val id: String,

    // --- Background ---------------------------------------------------------
    val backgroundTop: Int,
    val backgroundBottom: Int,
    val cloudColor: Int,

    // --- Platforms (one colour per PlatformType) ----------------------------
    val platformStatic: Int,
    val platformMoving: Int,
    val platformVanishing: Int,
    val platformFragile: Int,
    val platformRare: Int,

    // --- Hazards ------------------------------------------------------------
    val monsterColor: Int,
    val monsterDarkColor: Int,
    val spikeColor: Int,

    // --- Goal line ----------------------------------------------------------
    val goalColor: Int,

    // --- HUD text drawn on the canvas --------------------------------------
    val canvasTextColor: Int
) {
    companion object {

        const val DEFAULT_THEME_ID = "bg_doodle_paper"

        /**
         * Single source of truth for every gameplay theme.
         *
         * The ids match the background inventory items in
         * GameRepository.defaultShopItems(), so buying a background in the
         * shop swaps the entire gameplay palette.
         */
        val byId: Map<String, JumperTheme> = listOf(
            JumperTheme(
                id = "bg_doodle_paper",
                backgroundTop = Color.rgb(79, 195, 247),
                backgroundBottom = Color.rgb(25, 118, 210),
                cloudColor = Color.argb(85, 255, 255, 255),
                platformStatic = Color.rgb(102, 187, 106),
                platformMoving = Color.rgb(66, 165, 245),
                platformVanishing = Color.rgb(171, 71, 188),
                platformFragile = Color.rgb(239, 112, 59),
                platformRare = Color.rgb(255, 213, 79),
                monsterColor = Color.rgb(239, 83, 80),
                monsterDarkColor = Color.rgb(198, 40, 40),
                spikeColor = Color.rgb(198, 40, 40),
                goalColor = Color.rgb(255, 238, 88),
                canvasTextColor = Color.WHITE
            ),
            JumperTheme(
                id = "bg_forest",
                backgroundTop = Color.rgb(129, 199, 132),
                backgroundBottom = Color.rgb(46, 125, 50),
                cloudColor = Color.argb(70, 255, 255, 255),
                platformStatic = Color.rgb(141, 110, 99),
                platformMoving = Color.rgb(121, 85, 72),
                platformVanishing = Color.rgb(174, 213, 129),
                platformFragile = Color.rgb(255, 167, 38),
                platformRare = Color.rgb(255, 235, 59),
                monsterColor = Color.rgb(216, 67, 21),
                monsterDarkColor = Color.rgb(141, 37, 0),
                spikeColor = Color.rgb(93, 64, 55),
                goalColor = Color.rgb(255, 241, 118),
                canvasTextColor = Color.WHITE
            ),
            JumperTheme(
                id = "bg_sky_high",
                backgroundTop = Color.rgb(179, 229, 252),
                backgroundBottom = Color.rgb(30, 136, 229),
                cloudColor = Color.argb(120, 255, 255, 255),
                platformStatic = Color.rgb(255, 255, 255),
                platformMoving = Color.rgb(129, 212, 250),
                platformVanishing = Color.rgb(206, 147, 216),
                platformFragile = Color.rgb(255, 138, 101),
                platformRare = Color.rgb(255, 213, 79),
                monsterColor = Color.rgb(236, 64, 122),
                monsterDarkColor = Color.rgb(173, 20, 87),
                spikeColor = Color.rgb(120, 144, 156),
                goalColor = Color.rgb(255, 235, 59),
                canvasTextColor = Color.rgb(13, 71, 161)
            ),
            JumperTheme(
                id = "bg_underwater",
                backgroundTop = Color.rgb(77, 208, 225),
                backgroundBottom = Color.rgb(0, 96, 100),
                cloudColor = Color.argb(60, 224, 247, 250),
                platformStatic = Color.rgb(255, 224, 130),
                platformMoving = Color.rgb(77, 182, 172),
                platformVanishing = Color.rgb(128, 222, 234),
                platformFragile = Color.rgb(255, 138, 101),
                platformRare = Color.rgb(255, 241, 118),
                monsterColor = Color.rgb(255, 112, 67),
                monsterDarkColor = Color.rgb(191, 54, 12),
                spikeColor = Color.rgb(38, 50, 56),
                goalColor = Color.rgb(255, 241, 118),
                canvasTextColor = Color.WHITE
            ),
            JumperTheme(
                id = "bg_space",
                backgroundTop = Color.rgb(48, 63, 159),
                backgroundBottom = Color.rgb(13, 20, 58),
                cloudColor = Color.argb(70, 197, 202, 233),
                platformStatic = Color.rgb(120, 144, 156),
                platformMoving = Color.rgb(92, 107, 192),
                platformVanishing = Color.rgb(149, 117, 205),
                platformFragile = Color.rgb(255, 112, 67),
                platformRare = Color.rgb(255, 213, 79),
                monsterColor = Color.rgb(0, 230, 118),
                monsterDarkColor = Color.rgb(0, 137, 123),
                spikeColor = Color.rgb(176, 190, 197),
                goalColor = Color.rgb(255, 241, 118),
                canvasTextColor = Color.WHITE
            ),
            JumperTheme(
                id = "bg_lava",
                backgroundTop = Color.rgb(255, 138, 101),
                backgroundBottom = Color.rgb(127, 16, 16),
                cloudColor = Color.argb(70, 255, 204, 188),
                platformStatic = Color.rgb(66, 66, 66),
                platformMoving = Color.rgb(97, 97, 97),
                platformVanishing = Color.rgb(255, 167, 38),
                platformFragile = Color.rgb(255, 87, 34),
                platformRare = Color.rgb(255, 235, 59),
                monsterColor = Color.rgb(255, 23, 68),
                monsterDarkColor = Color.rgb(183, 28, 28),
                spikeColor = Color.rgb(33, 33, 33),
                goalColor = Color.rgb(255, 245, 157),
                canvasTextColor = Color.WHITE
            )
        ).associateBy { it.id }

        /** Returns the theme for [id], falling back to the default theme. */
        fun resolve(id: String?): JumperTheme =
            byId[id] ?: byId.getValue(DEFAULT_THEME_ID)
    }
}
