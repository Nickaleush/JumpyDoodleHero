package com.businessdoomguy.minigamestarter.games.jumper

/**
 * Registry of in-game background images.
 *
 * ---------------------------------------------------------------------------
 * BACKGROUND ART ENTRY POINT
 * ---------------------------------------------------------------------------
 * Each shop background item id maps to a full-screen background drawable.
 * The game draws this image stretched to fill the screen, exactly like the
 * menu background (`bg_jumper_menu` -> `default_bg`).
 *
 * To add real art for a background: drop a PNG into `res/drawable` and put
 * its resource name here. Backgrounds whose `resName` is null (no art yet)
 * fall back to the procedural gradient from [JumperTheme].
 *
 * The ids match the background inventory items in
 * GameRepository.defaultShopItems(), so buying a background in the shop
 * swaps the gameplay background image.
 */
object JumperBackgrounds {

    private val resNameById: Map<String, String?> = mapOf(
        // Default background ships with real art.
        "bg_doodle_paper" to "default_bg",
        // Add PNGs named like below to enable these; null = gradient fallback.
        "bg_forest" to "game_bg_forest",
        "bg_sky_high" to "game_bg_sky_high",
        "bg_underwater" to "game_bg_underwater",
        "bg_space" to "game_bg_space",
        "bg_lava" to "game_bg_lava"
    )

    fun resNameFor(backgroundId: String?): String? {
        if (backgroundId == null) return resNameById["bg_doodle_paper"]
        return resNameById[backgroundId]
    }
}
