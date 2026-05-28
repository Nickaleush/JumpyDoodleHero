package com.businessdoomguy.minigamestarter.games.jumper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Type of a platform. Defined at top level so both [JumperGameView] and the
 * sprite registry below can reference it.
 */
enum class PlatformType { Static, Moving, Vanishing, Fragile, Rare }

/** Type of a collectible booster. */
enum class BoosterType { Spring, Jetpack, Shield, Magnet, Star }

/** Type of a hazard. */
enum class HazardType { Monster, Spike }

/**
 * Central sprite registry and bitmap cache for the Jumper gameplay elements
 * (platforms, hazards, boosters).
 *
 * ---------------------------------------------------------------------------
 * ART ENTRY POINT
 * ---------------------------------------------------------------------------
 * Every gameplay element is drawn from a real artwork sprite in
 * `res/drawable`. The drawable resource name for each element is listed in
 * the [Sprite] enum below. To swap any element's art, drop a new file with
 * the same name into `res/drawable` — no code change required.
 *
 * Some element variants intentionally reuse the same artwork until dedicated
 * art is supplied (see the comments on each [Sprite] entry). Point them at a
 * new resource name here once the art exists.
 *
 * Bitmaps are decoded once and cached for the lifetime of the process.
 */
enum class Sprite(val resName: String) {
    // --- Platforms ---------------------------------------------------------
    /** Static / Vanishing / Rare platforms — supplied "simple" art. */
    PlatformSimple("platform_simple"),
    /** Moving platform — supplied dedicated "moving" art. */
    PlatformMoving("platform_moving"),
    /** Fragile platform — supplied "frozen/ice" art (ice = breaks on touch). */
    PlatformFrozen("platform_frozen"),

    // --- Hazards -----------------------------------------------------------
    /** Monster hazard — supplied UFO enemy art. */
    HazardEnemy("hazard_enemy"),
    /** Spike hazard — supplied spiked bomb art. */
    HazardBomb("hazard_bomb"),

    // --- Boosters ----------------------------------------------------------
    BoosterSpring("booster_spring"),
    BoosterJetpack("booster_jetpack"),
    BoosterShield("booster_shield"),

    // --- Collectibles ------------------------------------------------------
    /** In-game collectible coin — uses the supplied `ic_coin` art. */
    CoinPickup("ic_coin");

    companion object {
        /**
         * Maps a [PlatformType] to its sprite. Several platform behaviours
         * currently share the "simple" artwork; give them their own [Sprite]
         * entry + drawable when dedicated art is available.
         */
        fun forPlatform(type: PlatformType): Sprite = when (type) {
            PlatformType.Static -> PlatformSimple
            PlatformType.Moving -> PlatformMoving
            // Both Vanishing and Fragile disappear when the hero lands on
            // them, so both use the frozen/ice art to signal "do not linger".
            PlatformType.Vanishing -> PlatformFrozen
            PlatformType.Rare -> PlatformSimple
            PlatformType.Fragile -> PlatformFrozen
        }

        /** Maps a [HazardType] to its sprite. */
        fun forHazard(type: HazardType): Sprite = when (type) {
            HazardType.Monster -> HazardEnemy
            HazardType.Spike -> HazardBomb
        }

        /**
         * Maps a [BoosterType] to its sprite. Magnet and Star reuse the
         * shield artwork as a placeholder until dedicated art is supplied.
         */
        /**
         * Maps a [BoosterType] to its sprite, or null when there is no
         * dedicated art yet. Returning null (instead of reusing the shield
         * sprite) prevents Magnet/Star from being mistaken for a Shield — the
         * caller draws a distinct procedural badge for null.
         */
        fun forBooster(type: BoosterType): Sprite? = when (type) {
            BoosterType.Spring -> BoosterSpring
            BoosterType.Jetpack -> BoosterJetpack
            BoosterType.Shield -> BoosterShield
            BoosterType.Magnet -> null
            BoosterType.Star -> null
        }
    }
}

/**
 * Loads and caches [Sprite] bitmaps. One instance is created per view that
 * needs to draw sprites; the underlying decode is cheap because results are
 * cached per [Sprite].
 *
 * @param context application context, used to resolve drawable resources.
 */
class JumperSpriteCache(private val context: Context) {

    private val cache = HashMap<Sprite, Bitmap?>()
    private val namedCache = HashMap<String, Bitmap?>()

    /**
     * Returns the decoded bitmap for [sprite], or null if the drawable is
     * missing (the caller is then responsible for a safe fallback — in this
     * project every gameplay sprite ships, so null should not occur).
     */
    fun get(sprite: Sprite): Bitmap? {
        if (cache.containsKey(sprite)) return cache[sprite]
        val resId = context.resources.getIdentifier(
            sprite.resName, "drawable", context.packageName
        )
        val bitmap = if (resId != 0) decode(resId) else null
        cache[sprite] = bitmap
        return bitmap
    }

    /**
     * Returns the decoded bitmap for an arbitrary drawable [resName], or null
     * if no such drawable exists. Used for swappable background images.
     */
    fun getByName(resName: String?): Bitmap? {
        if (resName == null) return null
        if (namedCache.containsKey(resName)) return namedCache[resName]
        val resId = context.resources.getIdentifier(
            resName, "drawable", context.packageName
        )
        val bitmap = if (resId != 0) decode(resId) else null
        namedCache[resName] = bitmap
        return bitmap
    }

    private fun decode(resId: Int): Bitmap? = runCatching {
        BitmapFactory.decodeResource(context.resources, resId)?.let { return it }
        val drawable: Drawable = context.getDrawable(resId) ?: return null
        if (drawable is BitmapDrawable) return drawable.bitmap
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 256
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(Canvas(bmp))
        bmp
    }.getOrNull()
}
