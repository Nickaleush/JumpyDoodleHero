package com.businessdoomguy.minigamestarter.feature.shop

import androidx.annotation.DrawableRes
import com.businessdoomguy.minigamestarter.R

/**
 * Maps a shop inventory item id to its badge artwork drawable.
 *
 * ---------------------------------------------------------------------------
 * ART ENTRY POINT (shop)
 * ---------------------------------------------------------------------------
 * The shop card shows a ready-made badge image for each item. To change a
 * badge, replace the drawable file; to add an item, add one entry here.
 *
 * Some items do not have dedicated badge art yet (`skin_dragon`,
 * `bg_doodle_paper`, `bg_sky_high`, `bg_space`); [badgeFor] returns 0 for
 * them and the shop falls back to the procedural [com.businessdoomguy
 * .minigamestarter.games.jumper.JumperPreviewView] preview.
 */
object ShopArtwork {

    private val badges: Map<String, Int> = mapOf(
        // Hero skins
        "skin_doodle_boy" to R.drawable.shop_skin_doodle_boy,
        "skin_ninja" to R.drawable.shop_skin_ninja,
        "skin_robot" to R.drawable.shop_skin_robot,
        "skin_space_cat" to R.drawable.shop_skin_space_cat,
        "skin_dragon" to R.drawable.shop_skin_dragon,
        // Backgrounds
        "bg_forest" to R.drawable.shop_bg_forest,
        "bg_underwater" to R.drawable.shop_bg_underwater,
        "bg_lava" to R.drawable.shop_bg_lava,
        "bg_sky_high" to R.drawable.shop_bg_sky_high,
        "bg_space" to R.drawable.shop_bg_space,
        "bg_doodle_paper" to R.drawable.shop_bg_default,
    )

    /**
     * Returns the badge drawable resource for [itemId], or 0 if the item has
     * no dedicated badge art (the caller should then fall back to the
     * procedural preview).
     */
    @DrawableRes
    fun badgeFor(itemId: String): Int = badges[itemId] ?: 0
}
