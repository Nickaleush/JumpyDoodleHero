package com.businessdoomguy.minigamestarter.games.jumper

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Small, self-contained preview widget for the shop.
 *
 * It can render either:
 *  - a hero skin ([showSkin]) — the actual [JumperSkinRenderer] output, so the
 *    shop shows exactly what the player will get in-game; or
 *  - a background theme ([showBackground]) — the theme gradient plus a couple
 *    of sample platforms.
 *
 * Reusing the real renderer / theme means the shop preview can never drift
 * out of sync with the gameplay visuals.
 */
class JumperPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class Mode { Skin, Background }

    private val skinRenderer = JumperSkinRenderer(context.applicationContext)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var mode = Mode.Skin
    private var skin: JumperSkin = JumperSkins.resolve(null)
    private var theme: JumperTheme = JumperTheme.resolve(null)

    /** Renders [skinId] as a hero preview. */
    fun showSkin(skinId: String) {
        mode = Mode.Skin
        skin = JumperSkins.resolve(skinId)
        // A neutral backdrop so every skin reads clearly.
        theme = JumperTheme.resolve(null)
        invalidate()
    }

    /** Renders [backgroundId] as a background/theme preview. */
    fun showBackground(backgroundId: String) {
        mode = Mode.Background
        theme = JumperTheme.resolve(backgroundId)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Rounded background using the theme gradient.
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            theme.backgroundTop, theme.backgroundBottom,
            Shader.TileMode.CLAMP
        )
        val radius = h * 0.18f
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        bgPaint.shader = null

        when (mode) {
            Mode.Skin -> {
                val heroRadius = h * 0.30f
                skinRenderer.draw(canvas, skin, w / 2f, h * 0.52f, heroRadius)
            }
            Mode.Background -> {
                // A couple of sample platforms to show the theme's palette.
                platformPaint.color = theme.platformStatic
                drawSamplePlatform(canvas, w * 0.16f, h * 0.62f, w * 0.34f)
                platformPaint.color = theme.platformMoving
                drawSamplePlatform(canvas, w * 0.54f, h * 0.40f, w * 0.30f)
                platformPaint.color = theme.platformRare
                drawSamplePlatform(canvas, w * 0.30f, h * 0.24f, w * 0.26f)
            }
        }
    }

    private fun drawSamplePlatform(canvas: Canvas, left: Float, top: Float, pWidth: Float) {
        val pHeight = height * 0.12f
        rect.set(left, top, left + pWidth, top + pHeight)
        canvas.drawRoundRect(rect, pHeight / 2f, pHeight / 2f, platformPaint)
    }
}
