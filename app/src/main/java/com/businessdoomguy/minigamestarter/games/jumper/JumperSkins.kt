package com.businessdoomguy.minigamestarter.games.jumper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Hero skin system for Jumper.
 *
 * ---------------------------------------------------------------------------
 * DESIGN ENTRY POINT
 * ---------------------------------------------------------------------------
 * Everything about how a hero skin LOOKS lives in this file. To restyle a
 * skin, change its [JumperSkin] entry in [JumperSkins.byId]. To add a new
 * skin, add one entry here and one matching `InventoryItemEntity` in
 * GameRepository.defaultShopItems(). No other file needs to change.
 *
 * SPRITE ARTWORK
 * --------------
 * Each skin names a sprite drawable via [JumperSkin.spriteResName]. When a
 * drawable with that name exists in `res/drawable` (PNG / WebP / vector), the
 * renderer draws it. While the artwork is not yet added, the renderer falls
 * back to a procedural vector character so the game is always playable and
 * every skin still looks distinct.
 *
 * To ship real art: drop files named e.g. `hero_doodle_boy.png` into
 * `res/drawable` (lowercase, digits, underscores only) — no code change is
 * required. See `docs/HERO_SPRITES.md` for the full list of names.
 *
 * Per the technical spec, skins 1-2 are the "real" ones; skins 3-5 also have
 * full fallback artwork so the project ships a complete look.
 */

/**
 * The shape archetype used for the PROCEDURAL FALLBACK when no sprite asset
 * is present. Each archetype produces a recognisably different silhouette.
 */
enum class HeroShape {
    /** Classic round doodle character. */
    Round,
    /** Angular ninja with a headband tail. */
    Ninja,
    /** Boxy robot with antenna. */
    Robot,
    /** Cat with pointy ears and a helmet dome. */
    SpaceCat,
    /** Dragon with small wings. */
    Dragon
}

/**
 * Immutable visual description of a single hero skin.
 *
 * @property id            inventory id, must match the shop item id
 * @property spriteResName drawable resource name for the sprite artwork; when
 *                         the drawable is absent the procedural [shape] is
 *                         drawn instead
 * @property shape         procedural-fallback silhouette, see [HeroShape]
 * @property bodyColor     main fill colour (fallback)
 * @property accentColor   secondary colour (fallback accessory)
 * @property outlineColor  stroke colour around the body (fallback)
 * @property eyeColor      colour of the eyes (fallback)
 */
data class JumperSkin(
    val id: String,
    val spriteResName: String,
    val shape: HeroShape,
    val bodyColor: Int,
    val accentColor: Int,
    val outlineColor: Int,
    val eyeColor: Int = Color.rgb(38, 38, 38)
)

object JumperSkins {

    const val DEFAULT_SKIN_ID = "skin_doodle_boy"

    /**
     * The single source of truth for every skin's appearance.
     * Edit colours / shapes here to restyle the heroes.
     */
    val byId: Map<String, JumperSkin> = listOf(
        JumperSkin(
            id = "skin_doodle_boy",
            spriteResName = "hero_doodle_boy",
            shape = HeroShape.Round,
            bodyColor = Color.rgb(255, 183, 77),
            accentColor = Color.rgb(255, 138, 101),
            outlineColor = Color.rgb(117, 76, 36)
        ),
        JumperSkin(
            id = "skin_ninja",
            spriteResName = "hero_ninja",
            shape = HeroShape.Ninja,
            bodyColor = Color.rgb(55, 71, 79),
            accentColor = Color.rgb(229, 57, 53),
            outlineColor = Color.rgb(20, 28, 33),
            eyeColor = Color.rgb(255, 255, 255)
        ),
        JumperSkin(
            id = "skin_robot",
            spriteResName = "hero_robot",
            shape = HeroShape.Robot,
            bodyColor = Color.rgb(176, 190, 197),
            accentColor = Color.rgb(0, 188, 212),
            outlineColor = Color.rgb(69, 90, 100),
            eyeColor = Color.rgb(0, 188, 212)
        ),
        JumperSkin(
            id = "skin_space_cat",
            spriteResName = "hero_space_cat",
            shape = HeroShape.SpaceCat,
            bodyColor = Color.rgb(186, 104, 200),
            accentColor = Color.rgb(225, 190, 231),
            outlineColor = Color.rgb(94, 53, 102)
        ),
        JumperSkin(
            id = "skin_dragon",
            spriteResName = "hero_dragon",
            shape = HeroShape.Dragon,
            bodyColor = Color.rgb(102, 187, 106),
            accentColor = Color.rgb(255, 213, 79),
            outlineColor = Color.rgb(46, 92, 48)
        )
    ).associateBy { it.id }

    /** Returns the skin for [id], falling back to the default skin. */
    fun resolve(id: String?): JumperSkin =
        byId[id] ?: byId.getValue(DEFAULT_SKIN_ID)
}

/**
 * Renders a [JumperSkin] onto a Canvas.
 *
 * Drawing strategy, per skin:
 *  1. If a sprite drawable named [JumperSkin.spriteResName] exists, draw it.
 *  2. Otherwise, draw the procedural fallback character.
 *
 * Sprite bitmaps are decoded once and cached. The renderer is reused by both
 * [JumperGameView] and the shop preview, so gameplay and shop never diverge.
 *
 * @param context used to resolve and decode sprite drawables; the application
 *                context is recommended to avoid leaking an Activity.
 */
class JumperSkinRenderer(
    private val context: Context? = null
) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val eye = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val rect = RectF()
    private val srcRect = Rect()
    private val path = Path()

    /**
     * Per-skin sprite cache.
     * - missing key  -> not looked up yet
     * - value null   -> looked up, no sprite asset exists (use fallback)
     * - value bitmap -> sprite ready to draw
     */
    private val spriteCache = HashMap<String, Bitmap?>()

    /**
     * Draws [skin] centred at ([cx], [cy]) with the given body [radius].
     * Uses the sprite asset when available, otherwise the procedural shape.
     */
    fun draw(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        val sprite = spriteFor(skin)
        if (sprite != null) {
            drawSprite(canvas, sprite, cx, cy, radius)
            return
        }
        stroke.strokeWidth = radius * 0.18f
        when (skin.shape) {
            HeroShape.Round -> drawRound(canvas, skin, cx, cy, radius)
            HeroShape.Ninja -> drawNinja(canvas, skin, cx, cy, radius)
            HeroShape.Robot -> drawRobot(canvas, skin, cx, cy, radius)
            HeroShape.SpaceCat -> drawSpaceCat(canvas, skin, cx, cy, radius)
            HeroShape.Dragon -> drawDragon(canvas, skin, cx, cy, radius)
        }
    }

    /**
     * Resolves and caches the sprite bitmap for [skin], or null if no sprite
     * drawable is present (then the procedural fallback is used).
     */
    private fun spriteFor(skin: JumperSkin): Bitmap? {
        if (spriteCache.containsKey(skin.id)) return spriteCache[skin.id]
        val ctx = context
        var bitmap: Bitmap? = null
        if (ctx != null) {
            val resId = ctx.resources.getIdentifier(
                skin.spriteResName, "drawable", ctx.packageName
            )
            if (resId != 0) {
                bitmap = decodeDrawable(ctx, resId)
            }
        }
        spriteCache[skin.id] = bitmap
        return bitmap
    }

    /** Decodes a drawable resource (raster or vector) into a Bitmap. */
    private fun decodeDrawable(ctx: Context, resId: Int): Bitmap? {
        return runCatching {
            // Fast path for raster assets (PNG / WebP).
            BitmapFactory.decodeResource(ctx.resources, resId)?.let { return it }
            // Vector / shape drawables: rasterise into a bitmap.
            val drawable: Drawable = ctx.getDrawable(resId) ?: return null
            if (drawable is BitmapDrawable) return drawable.bitmap
            val fallbackSize = 256
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: fallbackSize
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: fallbackSize
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(c)
            bmp
        }.getOrNull()
    }

    /** Draws a sprite bitmap centred on the hero position. */
    /**
     * Draws the hero sprite bitmap centred on the hero position, preserving
     * the sprite's aspect ratio. Character sprites are typically taller than
     * wide, so the width is keyed to the physics radius and the height grows
     * proportionally — drawing into a square would squash the character.
     */
    private fun drawSprite(canvas: Canvas, sprite: Bitmap, cx: Float, cy: Float, radius: Float) {
        // Target width: a bit wider than the physics circle for visual padding.
        val drawWidth = radius * 2.5f
        val aspect = sprite.height.toFloat() / sprite.width.toFloat()
        val drawHeight = drawWidth * aspect
        srcRect.set(0, 0, sprite.width, sprite.height)
        // Anchor the sprite so the physics centre sits a little above the
        // sprite's vertical centre (characters have their mass in the torso).
        rect.set(
            cx - drawWidth / 2f,
            cy - drawHeight * 0.55f,
            cx + drawWidth / 2f,
            cy + drawHeight * 0.45f
        )
        canvas.drawBitmap(sprite, srcRect, rect, spritePaint)
    }

    private fun drawEyes(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        eye.color = skin.eyeColor
        val dx = radius * 0.34f
        val dy = radius * 0.22f
        val r = radius * 0.16f
        canvas.drawCircle(cx - dx, cy - dy, r, eye)
        canvas.drawCircle(cx + dx, cy - dy, r, eye)
    }

    private fun drawRound(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        fill.color = skin.bodyColor
        stroke.color = skin.outlineColor
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)
        // Little feet.
        fill.color = skin.accentColor
        canvas.drawCircle(cx - radius * 0.55f, cy + radius * 0.85f, radius * 0.26f, fill)
        canvas.drawCircle(cx + radius * 0.55f, cy + radius * 0.85f, radius * 0.26f, fill)
        drawEyes(canvas, skin, cx, cy, radius)
    }

    private fun drawNinja(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        fill.color = skin.bodyColor
        stroke.color = skin.outlineColor
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)
        // Headband across the eyes.
        fill.color = skin.accentColor
        rect.set(cx - radius, cy - radius * 0.42f, cx + radius, cy - radius * 0.02f)
        canvas.drawRect(rect, fill)
        // Headband tail flapping to one side.
        path.reset()
        path.moveTo(cx + radius * 0.92f, cy - radius * 0.36f)
        path.lineTo(cx + radius * 1.55f, cy - radius * 0.62f)
        path.lineTo(cx + radius * 1.5f, cy - radius * 0.12f)
        path.close()
        canvas.drawPath(path, fill)
        drawEyes(canvas, skin, cx, cy, radius)
    }

    private fun drawRobot(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        fill.color = skin.bodyColor
        stroke.color = skin.outlineColor
        // Boxy body with rounded corners.
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawRoundRect(rect, radius * 0.3f, radius * 0.3f, fill)
        canvas.drawRoundRect(rect, radius * 0.3f, radius * 0.3f, stroke)
        // Antenna.
        fill.color = skin.accentColor
        canvas.drawRect(
            cx - radius * 0.08f, cy - radius * 1.5f,
            cx + radius * 0.08f, cy - radius, fill
        )
        canvas.drawCircle(cx, cy - radius * 1.55f, radius * 0.2f, fill)
        // Rectangular visor eyes.
        eye.color = skin.eyeColor
        canvas.drawRect(
            cx - radius * 0.5f, cy - radius * 0.36f,
            cx - radius * 0.12f, cy - radius * 0.04f, eye
        )
        canvas.drawRect(
            cx + radius * 0.12f, cy - radius * 0.36f,
            cx + radius * 0.5f, cy - radius * 0.04f, eye
        )
    }

    private fun drawSpaceCat(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        fill.color = skin.bodyColor
        stroke.color = skin.outlineColor
        // Pointy ears.
        path.reset()
        path.moveTo(cx - radius * 0.7f, cy - radius * 0.6f)
        path.lineTo(cx - radius * 0.95f, cy - radius * 1.35f)
        path.lineTo(cx - radius * 0.2f, cy - radius * 0.85f)
        path.moveTo(cx + radius * 0.7f, cy - radius * 0.6f)
        path.lineTo(cx + radius * 0.95f, cy - radius * 1.35f)
        path.lineTo(cx + radius * 0.2f, cy - radius * 0.85f)
        canvas.drawPath(path, fill)
        // Head.
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)
        // Helmet dome.
        fill.color = skin.accentColor
        fill.alpha = 90
        canvas.drawCircle(cx, cy - radius * 0.1f, radius * 1.05f, fill)
        fill.alpha = 255
        drawEyes(canvas, skin, cx, cy, radius)
    }

    private fun drawDragon(canvas: Canvas, skin: JumperSkin, cx: Float, cy: Float, radius: Float) {
        // Wings behind the body.
        fill.color = skin.accentColor
        path.reset()
        path.moveTo(cx - radius * 0.6f, cy)
        path.lineTo(cx - radius * 1.7f, cy - radius * 0.7f)
        path.lineTo(cx - radius * 1.5f, cy + radius * 0.5f)
        path.close()
        path.moveTo(cx + radius * 0.6f, cy)
        path.lineTo(cx + radius * 1.7f, cy - radius * 0.7f)
        path.lineTo(cx + radius * 1.5f, cy + radius * 0.5f)
        path.close()
        canvas.drawPath(path, fill)
        // Body.
        fill.color = skin.bodyColor
        stroke.color = skin.outlineColor
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)
        // Small horns.
        fill.color = skin.accentColor
        path.reset()
        path.moveTo(cx - radius * 0.4f, cy - radius * 0.85f)
        path.lineTo(cx - radius * 0.55f, cy - radius * 1.3f)
        path.lineTo(cx - radius * 0.15f, cy - radius * 0.95f)
        path.moveTo(cx + radius * 0.4f, cy - radius * 0.85f)
        path.lineTo(cx + radius * 0.55f, cy - radius * 1.3f)
        path.lineTo(cx + radius * 0.15f, cy - radius * 0.95f)
        canvas.drawPath(path, fill)
        drawEyes(canvas, skin, cx, cy, radius)
    }
}
