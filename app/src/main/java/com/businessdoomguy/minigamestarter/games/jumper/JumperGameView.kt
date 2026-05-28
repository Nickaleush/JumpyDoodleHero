package com.businessdoomguy.minigamestarter.games.jumper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class JumperGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    interface Callback {
        fun onHudChanged(level: Int, heightMeters: Int, targetMeters: Int, score: Int, shieldLives: Int)
        fun onGameFinished(result: JumperGameResult)

        /** Fired when the hero bounces off any platform. */
        fun onJumpSound() = Unit

        /** Fired when a booster (spring, jetpack, shield, magnet, star) is collected. */
        fun onBoosterSound() = Unit

        /** Fired when the hero stomps a monster from above. */
        fun onMonsterStompSound() = Unit

        /** Fired when the hero is hit by a hazard (shielded or fatal). */
        fun onHazardSound() = Unit
    }

    // PlatformType / BoosterType / HazardType are declared at top level in
    // JumperSprites.kt so the sprite registry can map them to artwork.

    private data class Platform(
        var x: Float,
        val yMeters: Float,
        val width: Float,
        val type: PlatformType,
        var moveDirection: Float = 1f,
        var touched: Boolean = false,
        var visible: Boolean = true,
        val safeRoute: Boolean = false
    )

    private data class Booster(
        var x: Float,
        val yMeters: Float,
        val type: BoosterType,
        var collected: Boolean = false
    )

    private data class Hazard(
        var x: Float,
        val yMeters: Float,
        val type: HazardType,
        val width: Float,
        var active: Boolean = true
    )

    private data class Coin(
        var x: Float,
        val yMeters: Float,
        var collected: Boolean = false
    )

    var callback: Callback? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(85, 255, 255, 255) }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 26f
        isFakeBoldText = true
    }
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 238, 88) }

    // --- Sprite rendering (platforms, hazards, boosters) -------------------
    // All gameplay elements are drawn from artwork in res/drawable via the
    // shared sprite cache. See JumperSprites.kt to remap art.
    private val spriteCache = JumperSpriteCache(context.applicationContext)
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val spriteSrcRect = Rect()
    private val spriteDstRect = RectF()
    /**
     * Global multiplier that enlarges every gameplay element (player, platforms,
     * hazards, boosters and their collision bounds) proportionally. Bumping this
     * single value makes the whole game read bigger/chunkier without changing the
     * relative balance, because both the drawn sprites AND the collision/route
     * widths are scaled by it together.
     *
     * Safety: enlarging platforms only ever makes the level EASIER (the
     * reachability check adds a share of the landing width), and every platform
     * width is additionally clamped by [safePlatformWidth] so it can never grow
     * so wide that the route stops fitting on screen. So raising this value
     * cannot produce an unbeatable level.
     */
    private val elementScale = 1.6f

    /** On-screen size of a hazard sprite (its larger side), in pixels. */
    private val hazardDrawSize = 96f * elementScale
    /** On-screen size of a booster sprite (its larger side), in pixels. */
    private val boosterDrawSize = 70f * elementScale
    /** On-screen size of an in-game coin sprite (its larger side), in pixels. */
    private val coinDrawSize = 56f * elementScale

    private var config = JumperLevelConfig.forLevel(1)
    private var random = Random(config.level * 997)

    private val metersToPx = 64f
    private val heroRadius = 28f * elementScale
    private val heroHeightMeters = 0.85f
    private val gravity = -24f
    private val baseJumpVelocity = 14.5f
    private val maxHorizontalSpeed = 780f

    // Passability guard rails. With the current jump velocity and gravity the
    // theoretical vertical reach is about 4.3 meters, but levels are generated
    // with a safety margin so every next platform is reachable without relying
    // on boosters, rare platforms, perfect input, or lucky platform movement.
    private val safeMinPlatformGapMeters = 1.75f
    private val safeMaxPlatformGapMeters = 3.38f
    private val safeMaxCenterDeltaPx = 330f
    private val safeSidePaddingPx = 28f

    /**
     * Width of the playfield the current level was generated for. Used by
     * [safePlatformWidth] to cap how wide a (scaled) platform may become so the
     * route always fits on screen with room to move.
     */
    private var currentGameWidth = 720f

    private var heroX = 0f
    private var heroY = 0f
    private var previousHeroY = 0f
    private var heroVelocityY = 0f
    // Horizontal control. manualDirection is -1 (left), 0 (idle) or 1 (right),
    // produced by pressing/holding the left or right side of the screen.
    private var manualDirection = 0f
    private var accelerometerDirection = 0f
    // Edge-press control: a finger on the left half moves the hero left, a
    // finger on the right half moves it right. We track active pointer ids so
    // multi-touch (e.g. lifting one finger while another stays down) behaves
    // predictably. The dead zone in the screen centre avoids accidental input.
    private val edgeDeadZoneFraction = 0.04f
    private val leftPointers = mutableSetOf<Int>()
    private val rightPointers = mutableSetOf<Int>()
    private var cameraY = 0f
    private var highestMeters = 0
    private var score = 0
    private var bonusScore = 0
    private var shieldLives = 0
    private var jetpackLeft = 0f
    private var magnetLeft = 0f
    private var isRunning = false
    private var isPaused = false
    private var isFinished = false
    private var lastFrameNanos = 0L
    private var lastHudScore = -1
    private var lastHudHeight = -1
    private var lastHudShield = -1
    // Appearance is fully driven by JumperTheme + JumperSkin. To restyle the
    // game, edit JumperTheme.kt / JumperSkins.kt — no change is needed here.
    private var theme: JumperTheme = JumperTheme.resolve(null)
    private var skin: JumperSkin = JumperSkins.resolve(null)
    private val skinRenderer = JumperSkinRenderer(context.applicationContext)
    // Swappable full-screen background image (chosen in the shop). Null when
    // the selected background has no dedicated art yet -> gradient fallback.
    private var backgroundBitmap: android.graphics.Bitmap? =
        spriteCache.getByName(JumperBackgrounds.resNameFor(null))
    private val backgroundSrcRect = Rect()
    private val backgroundDstRect = RectF()
    private var jumpCount = 0
    private var springUses = 0
    private var jetpackUses = 0
    private var monsterKills = 0
    private var starsCollected = 0
    private var hazardHits = 0
    private var coinsCollected = 0

    private val platforms = mutableListOf<Platform>()
    private val boosters = mutableListOf<Booster>()
    private val hazards = mutableListOf<Hazard>()
    private val coins = mutableListOf<Coin>()

    fun setAppearance(skinId: String, backgroundId: String) {
        skin = JumperSkins.resolve(skinId)
        theme = JumperTheme.resolve(backgroundId)
        // Resolve the swappable background image for the selected background.
        // Null when that background has no dedicated art yet -> gradient.
        backgroundBitmap = spriteCache.getByName(JumperBackgrounds.resNameFor(backgroundId))
        invalidate()
    }

    fun startLevel(level: Int) {
        config = JumperLevelConfig.forLevel(level.coerceIn(1, 45))
        random = Random(config.level * 997)
        resetWorld()
        isRunning = true
        isPaused = false
        isFinished = false
        lastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
        invalidate()
    }

    fun setPaused(paused: Boolean) {
        if (!isRunning || isFinished) return
        isPaused = paused
        if (!paused) {
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun restart() {
        startLevel(config.level)
    }

    fun stopGameLoop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun setTilt(rawX: Float) {
        accelerometerDirection = (-rawX / 5.5f).coerceIn(-1f, 1f)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning || isPaused || isFinished) return

        val dt = if (lastFrameNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
        }
        lastFrameNanos = frameTimeNanos

        update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawGoalLine(canvas)
        drawBoosters(canvas)
        drawHazards(canvas)
        drawPlatforms(canvas)
        drawCoins(canvas)
        drawHero(canvas)
    }

    /**
     * Edge-press horizontal control.
     *
     * Pressing and holding the left side of the screen moves the hero left;
     * the right side moves it right. Releasing the finger stops the movement.
     * A small dead zone in the centre prevents accidental nudges. Multi-touch
     * is supported: each pointer is classified independently, so the hero
     * keeps moving as long as at least one finger is held on a side.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val index = event.actionIndex
                classifyPointer(event.getPointerId(index), event.getX(index))
                updateManualDirection()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // A finger may slide across the centre line; re-classify every
                // active pointer on each move so the side it controls updates.
                for (i in 0 until event.pointerCount) {
                    classifyPointer(event.getPointerId(i), event.getX(i))
                }
                updateManualDirection()
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                leftPointers.remove(id)
                rightPointers.remove(id)
                updateManualDirection()
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                leftPointers.clear()
                rightPointers.clear()
                updateManualDirection()
                return true
            }
        }
        return true
    }

    /** Assigns a pointer to the left side, right side, or neither (dead zone). */
    private fun classifyPointer(pointerId: Int, x: Float) {
        leftPointers.remove(pointerId)
        rightPointers.remove(pointerId)
        if (width <= 0) return
        val center = width / 2f
        val deadZone = width * edgeDeadZoneFraction
        when {
            x < center - deadZone -> leftPointers.add(pointerId)
            x > center + deadZone -> rightPointers.add(pointerId)
            else -> Unit // centre dead zone: no movement
        }
    }

    /**
     * Resolves the held edges into a direction. If the player holds both
     * sides at once the inputs cancel out, which feels natural and avoids
     * jitter.
     */
    private fun updateManualDirection() {
        val left = leftPointers.isNotEmpty()
        val right = rightPointers.isNotEmpty()
        manualDirection = when {
            left && !right -> -1f
            right && !left -> 1f
            else -> 0f
        }
    }

    private fun resetWorld() {
        val gameWidth = if (width > 0) width.toFloat() else 720f
        val profile = LevelGenerationProfile.forLevel(config.level)

        resetRuntimeState(gameWidth)

        var generated = false
        for (attempt in 0 until 10) {
            platforms.clear()
            boosters.clear()
            hazards.clear()
            coins.clear()
            random = Random(config.level * 997 + attempt * 7919)

            buildProductionLevel(gameWidth, profile)
            sanitizeHazardsAgainstRoute()
            sanitizeHazardsAgainstPlatforms()

            if (validateGeneratedWorld()) {
                generated = true
                break
            }
        }

        if (!generated) {
            platforms.clear()
            boosters.clear()
            hazards.clear()
            coins.clear()
            random = Random(config.level * 997)
            buildFallbackPassableLevel(gameWidth, profile)
            sanitizeHazardsAgainstRoute()
            sanitizeHazardsAgainstPlatforms()
        }

        sendHud(force = true)
    }

    private fun resetRuntimeState(gameWidth: Float) {
        currentGameWidth = gameWidth
        heroX = gameWidth / 2f
        heroY = 1.3f
        previousHeroY = heroY
        heroVelocityY = baseJumpVelocity
        cameraY = 0f
        highestMeters = 0
        bonusScore = 0
        score = 0
        shieldLives = 0
        jetpackLeft = 0f
        magnetLeft = 0f
        manualDirection = 0f
        accelerometerDirection = 0f
        leftPointers.clear()
        rightPointers.clear()
        lastHudScore = -1
        lastHudHeight = -1
        lastHudShield = -1
        jumpCount = 0
        springUses = 0
        jetpackUses = 0
        monsterKills = 0
        starsCollected = 0
        hazardHits = 0
        coinsCollected = 0
    }

    /**
     * Original production generator for Jumper.
     *
     * The important rule is that the level is generated as a verified playable
     * graph, not as random noise. There is one guaranteed route to the goal, but
     * it is intentionally hidden inside side lanes, risky platforms and optional
     * branches, so the level feels authored instead of looking like a ladder.
     */
    private fun buildProductionLevel(gameWidth: Float, profile: LevelGenerationProfile) {
        val startWidth = safePlatformWidth(190f * elementScale)
        var previousCenterX = gameWidth / 2f
        var previousY = 0.5f
        var previousType = PlatformType.Static

        platforms += Platform(
            x = previousCenterX - startWidth / 2f,
            yMeters = previousY,
            width = startWidth,
            type = PlatformType.Static,
            safeRoute = true
        )

        var platformIndex = 0
        var lane = 1 // 0 = left, 1 = center, 2 = right
        while (previousY < config.targetHeightMeters + 18f) {
            platformIndex += 1

            val gap = authoredRouteGap(platformIndex, profile)
            val nextY = previousY + gap
            val width = authoredRouteWidth(platformIndex, profile)

            lane = nextLane(lane, platformIndex, profile)
            val desiredCenter = authoredRouteCenter(
                previousCenterX = previousCenterX,
                lane = lane,
                gameWidth = gameWidth,
                platformWidth = width,
                gapMeters = gap,
                platformIndex = platformIndex,
                profile = profile
            )

            val centerX = clampToReachableAndScreen(
                desiredCenter = desiredCenter,
                previousCenterX = previousCenterX,
                gapMeters = gap,
                platformWidth = width,
                gameWidth = gameWidth,
                profile = profile
            )

            val type = chooseRoutePlatformType(nextY, platformIndex, previousType, profile)
            val routePlatform = Platform(
                x = centerX - width / 2f,
                yMeters = nextY,
                width = width,
                type = type,
                moveDirection = if (random.nextBoolean()) 1f else -1f,
                safeRoute = true
            )

            if (!canPlacePlatform(routePlatform, extraMarginPx = 10f)) {
                // A route platform must never overlap anything. If the authored
                // candidate collides with an optional branch from the previous
                // segment, remove the optional branch rather than weakening the
                // main route or crashing the game.
                platforms.removeAll { !it.safeRoute && platformsOverlap(it, routePlatform, extraMarginPx = 12f) }
            }
            platforms += routePlatform

            createOptionalBranchesForSegment(
                fromCenterX = previousCenterX,
                toCenterX = centerX,
                fromY = previousY,
                toY = nextY,
                gameWidth = gameWidth,
                segmentIndex = platformIndex,
                profile = profile
            )

            createHazardsForSegment(
                fromCenterX = previousCenterX,
                toCenterX = centerX,
                fromY = previousY,
                toY = nextY,
                gameWidth = gameWidth,
                segmentIndex = platformIndex,
                profile = profile
            )

            maybeCreateBoosterOnPlatform(centerX, nextY, platformIndex, profile, routePlatform.safeRoute)
            maybeCreateCoinsOnPlatform(centerX, nextY, platformIndex)

            previousCenterX = centerX
            previousY = nextY
            previousType = type
        }
    }

    private fun buildFallbackPassableLevel(gameWidth: Float, profile: LevelGenerationProfile) {
        val startWidth = safePlatformWidth(190f * elementScale)
        var centerX = gameWidth / 2f
        var y = 0.5f
        platforms += Platform(centerX - startWidth / 2f, y, startWidth, PlatformType.Static, safeRoute = true)

        var index = 0
        while (y < config.targetHeightMeters + 18f) {
            index += 1
            val gap = (profile.minGap + 0.22f).coerceAtMost(maxSafeVerticalGapMeters() - 0.14f)
            y += gap
            val width = safePlatformWidth((profile.routeWidth + 20f).coerceIn(112f, 172f) * elementScale)
            val maxDelta = min(profile.maxCenterDeltaPx * 0.52f, maxReachableCenterDeltaPx(gap) * 0.68f)
            val direction = if (index % 2 == 0) 1f else -1f
            centerX = (centerX + direction * maxDelta).coerceIn(safeSidePaddingPx + width / 2f, gameWidth - safeSidePaddingPx - width / 2f)
            platforms += Platform(centerX - width / 2f, y, width, PlatformType.Static, safeRoute = true)
        }
    }

    private data class LevelGenerationProfile(
        val minGap: Float,
        val maxGap: Float,
        val maxCenterDeltaPx: Float,
        val routeWidth: Float,
        val branchChance: Float,
        val secondBranchChance: Float,
        val routeRiskChance: Float,
        val hazardPressure: Float,
        val sideLaneBias: Float,
        val forcedRecoveryEvery: Int,
        val horizontalTension: Float
    ) {
        companion object {
            fun forPattern(pattern: Int): LevelGenerationProfile {
                return when (pattern.coerceIn(1, 5)) {
                    1 -> LevelGenerationProfile(
                        minGap = 2.46f,
                        maxGap = 2.86f,
                        maxCenterDeltaPx = 250f,
                        routeWidth = 160f,
                        branchChance = 0.30f,
                        secondBranchChance = 0.06f,
                        routeRiskChance = 0.00f,
                        hazardPressure = 0.22f,
                        sideLaneBias = 0.42f,
                        forcedRecoveryEvery = 9,
                        horizontalTension = 0.70f
                    )
                    2 -> LevelGenerationProfile(
                        minGap = 2.56f,
                        maxGap = 3.10f,
                        maxCenterDeltaPx = 305f,
                        routeWidth = 138f,
                        branchChance = 0.50f,
                        secondBranchChance = 0.16f,
                        routeRiskChance = 0.22f,
                        hazardPressure = 0.42f,
                        sideLaneBias = 0.58f,
                        forcedRecoveryEvery = 9,
                        horizontalTension = 0.84f
                    )
                    3 -> LevelGenerationProfile(
                        minGap = 2.64f,
                        maxGap = 3.25f,
                        maxCenterDeltaPx = 345f,
                        routeWidth = 122f,
                        branchChance = 0.66f,
                        secondBranchChance = 0.26f,
                        routeRiskChance = 0.38f,
                        hazardPressure = 0.62f,
                        sideLaneBias = 0.72f,
                        forcedRecoveryEvery = 10,
                        horizontalTension = 0.98f
                    )
                    4 -> LevelGenerationProfile(
                        minGap = 2.72f,
                        maxGap = 3.34f,
                        maxCenterDeltaPx = 365f,
                        routeWidth = 112f,
                        branchChance = 0.74f,
                        secondBranchChance = 0.36f,
                        routeRiskChance = 0.52f,
                        hazardPressure = 0.78f,
                        sideLaneBias = 0.84f,
                        forcedRecoveryEvery = 10,
                        horizontalTension = 1.08f
                    )
                    else -> LevelGenerationProfile(
                        minGap = 2.78f,
                        maxGap = 3.38f,
                        maxCenterDeltaPx = 382f,
                        routeWidth = 104f,
                        branchChance = 0.80f,
                        secondBranchChance = 0.44f,
                        routeRiskChance = 0.66f,
                        hazardPressure = 0.92f,
                        sideLaneBias = 0.92f,
                        forcedRecoveryEvery = 11,
                        horizontalTension = 1.15f
                    )
                }
            }

            /**
             * Builds the generation profile for a concrete level.
             *
             * The level is mapped to one of the five base archetypes
             * (`forPattern`), then a CONTINUOUS difficulty ramp is applied so
             * that, e.g., level 11 is noticeably harder than level 6 even
             * though both share pattern archetype 1. This removes the old
             * "levels 6-45 feel identical" problem.
             *
             * The ramp is intentionally gentle and capped: it never pushes a
             * value past what the physics-based passability checks allow, so
             * every level stays completable.
             *
             * @param level 1..45
             */
            fun forLevel(level: Int): LevelGenerationProfile {
                val clampedLevel = level.coerceIn(1, 45)
                val pattern = ((clampedLevel - 1) % 5) + 1
                val base = forPattern(pattern)

                // ramp: ~0.0 on the first levels, growing toward 1.0 by 45.
                // Difficulty climbs MONOTONICALLY with the absolute level, so
                // each level is a little harder than the one before regardless of
                // which of the five archetypes it reuses.
                val ramp = ((clampedLevel - 1) / 44f).coerceIn(0f, 1f)

                return base.copy(
                    // Jumps reach a touch further and platforms get narrower over
                    // time, so precision matters more on higher levels.
                    maxGap = base.maxGap + 0.30f * ramp,
                    routeWidth = (base.routeWidth - 32f * ramp).coerceAtLeast(82f),
                    maxCenterDeltaPx = base.maxCenterDeltaPx + 52f * ramp,
                    // Harder levels offer FEWER optional branch platforms (fewer
                    // safety nets / alternative footholds), so the player must
                    // commit to the tighter main route.
                    branchChance = (base.branchChance - 0.20f * ramp).coerceAtLeast(0.12f),
                    secondBranchChance = (base.secondBranchChance - 0.12f * ramp).coerceAtLeast(0.04f),
                    // The main route uses riskier platform types (vanishing /
                    // fragile / moving) more often as levels climb.
                    routeRiskChance = (base.routeRiskChance + 0.26f * ramp).coerceAtMost(0.82f),
                    // Hazards get noticeably more frequent and aggressive.
                    hazardPressure = (base.hazardPressure + 0.24f * ramp).coerceAtMost(0.98f),
                    sideLaneBias = (base.sideLaneBias + 0.10f * ramp).coerceAtMost(0.96f),
                    horizontalTension = base.horizontalTension + 0.34f * ramp,
                    // The free "recovery" breather platforms become rarer the
                    // higher you climb.
                    forcedRecoveryEvery = (base.forcedRecoveryEvery + (4f * ramp).toInt())
                        .coerceIn(9, 15)
                )
            }
        }
    }

    private fun authoredRouteGap(platformIndex: Int, profile: LevelGenerationProfile): Float {
        val maxGap = min(profile.maxGap, maxSafeVerticalGapMeters() - 0.06f)
        val minGap = profile.minGap.coerceAtMost(maxGap - 0.18f)

        // A forced recovery jump is always short, giving the player a breather.
        if (platformIndex % profile.forcedRecoveryEvery == 0) {
            return minGap + random.nextFloat() * 0.16f
        }

        // --- Difficulty rhythm -------------------------------------------------
        // The route is built as repeating "tension arcs": each arc of
        // `arcLength` platforms ramps from an easy jump up to a tense one and
        // back down. A slow sine wave over `t` produces that arc shape, so the
        // player experiences a natural build-up / release instead of uniform
        // random gaps.
        val arcLength = 7
        val phase = (platformIndex % arcLength) / arcLength.toFloat()
        // sine arc: 0 at the ends of the arc, ~1 in the middle.
        val arc = kotlin.math.sin(phase * Math.PI).toFloat()

        // Every third arc is a "challenge burst" of consistently harder jumps,
        // which keeps long climbs from feeling monotonous.
        val isChallengeBurst = (platformIndex / arcLength) % 3 == 2
        val burstBoost = if (isChallengeBurst) 0.22f else 0f

        // The horizontalTension profile field (which the per-level ramp grows)
        // lifts the whole curve so higher levels demand bigger jumps overall.
        val tensionLift = (profile.horizontalTension - 0.70f) * 0.34f

        val noise = (random.nextFloat() - 0.5f) * 0.12f
        val t = (0.42f + arc * 0.5f + burstBoost + tensionLift + noise).coerceIn(0f, 1f)
        return minGap + (maxGap - minGap) * t
    }

    private fun authoredRouteWidth(platformIndex: Int, profile: LevelGenerationProfile): Float {
        val modifier = when (platformIndex % 10) {
            0 -> 18f
            1 -> 0f
            2 -> -10f
            3 -> -18f
            4 -> -6f
            5 -> -24f
            6 -> -12f
            7 -> -28f
            8 -> -16f
            else -> -34f
        }
        return safePlatformWidth((profile.routeWidth + modifier).coerceIn(84f, 166f) * elementScale)
    }

    private fun nextLane(currentLane: Int, platformIndex: Int, profile: LevelGenerationProfile): Int {
        if (platformIndex % profile.forcedRecoveryEvery == 0) return 1
        if (random.nextFloat() < profile.sideLaneBias) {
            return when (platformIndex % 6) {
                0 -> 0
                1 -> 2
                2 -> if (currentLane == 0) 2 else 0
                3 -> 1
                4 -> 2
                else -> 0
            }
        }
        return when (random.nextInt(3)) {
            0 -> currentLane
            1 -> (currentLane + 1).coerceAtMost(2)
            else -> (currentLane - 1).coerceAtLeast(0)
        }
    }

    private fun authoredRouteCenter(
        previousCenterX: Float,
        lane: Int,
        gameWidth: Float,
        platformWidth: Float,
        gapMeters: Float,
        platformIndex: Int,
        profile: LevelGenerationProfile
    ): Float {
        val halfWidth = platformWidth / 2f
        val minCenter = safeSidePaddingPx + halfWidth
        val maxCenter = gameWidth - safeSidePaddingPx - halfWidth
        val laneCenter = when (lane) {
            0 -> minCenter + (maxCenter - minCenter) * 0.17f
            2 -> minCenter + (maxCenter - minCenter) * 0.83f
            else -> minCenter + (maxCenter - minCenter) * 0.50f
        }
        val wobble = random.nextInt(-42, 43).toFloat()
        val aggressiveDelta = profile.maxCenterDeltaPx * (0.56f + 0.34f * profile.horizontalTension)
        val motifDelta = when (platformIndex % 7) {
            0 -> -aggressiveDelta
            1 -> aggressiveDelta * 0.72f
            2 -> aggressiveDelta
            3 -> -aggressiveDelta * 0.84f
            4 -> aggressiveDelta * 0.42f
            5 -> -aggressiveDelta
            else -> 0f
        }
        val motifCenter = previousCenterX + motifDelta
        return (laneCenter * 0.58f + motifCenter * 0.42f + wobble).coerceIn(minCenter, maxCenter)
    }

    private fun clampToReachableAndScreen(
        desiredCenter: Float,
        previousCenterX: Float,
        gapMeters: Float,
        platformWidth: Float,
        gameWidth: Float,
        profile: LevelGenerationProfile
    ): Float {
        val halfWidth = platformWidth / 2f
        val minCenter = safeSidePaddingPx + halfWidth
        val maxCenter = gameWidth - safeSidePaddingPx - halfWidth
        val reach = min(profile.maxCenterDeltaPx, maxReachableCenterDeltaPx(gapMeters))
        val minReach = (previousCenterX - reach).coerceAtLeast(minCenter)
        val maxReach = (previousCenterX + reach).coerceAtMost(maxCenter)
        return desiredCenter.coerceIn(minReach, maxReach)
    }

    private fun chooseRoutePlatformType(
        yMeters: Float,
        platformIndex: Int,
        previousType: PlatformType,
        profile: LevelGenerationProfile
    ): PlatformType {
        if (yMeters < 12f) return PlatformType.Static
        if (platformIndex % profile.forcedRecoveryEvery == 0) return PlatformType.Static
        if (random.nextFloat() > profile.routeRiskChance) return PlatformType.Static
        if (previousType != PlatformType.Static && random.nextFloat() < 0.68f) return PlatformType.Static

        val roll = random.nextFloat()
        return when {
            config.rarePlatforms && platformIndex % 13 == 0 && roll < 0.10f -> PlatformType.Rare
            config.fragilePlatforms && roll < 0.30f -> PlatformType.Fragile
            config.vanishingPlatforms && roll < 0.58f -> PlatformType.Vanishing
            config.movingPlatforms -> PlatformType.Moving
            else -> PlatformType.Static
        }
    }

    private fun createOptionalBranchesForSegment(
        fromCenterX: Float,
        toCenterX: Float,
        fromY: Float,
        toY: Float,
        gameWidth: Float,
        segmentIndex: Int,
        profile: LevelGenerationProfile
    ) {
        if (toY < 8f) return

        val branchCount = when {
            random.nextFloat() < profile.secondBranchChance -> 2
            random.nextFloat() < profile.branchChance -> 1
            else -> 0
        }

        repeat(branchCount) { branchIndex ->
            val type = chooseChallengePlatformType()
            val width = widthForPlatform(type)
            val halfWidth = width / 2f
            val minCenter = safeSidePaddingPx + halfWidth
            val maxCenter = gameWidth - safeSidePaddingPx - halfWidth
            val fraction = if (branchIndex == 0) {
                0.34f + random.nextFloat() * 0.22f
            } else {
                0.58f + random.nextFloat() * 0.22f
            }
            val y = fromY + (toY - fromY) * fraction
            val routeCenterAtY = fromCenterX + (toCenterX - fromCenterX) * fraction
            val routeDirection = if (toCenterX >= fromCenterX) 1f else -1f
            val side = if ((segmentIndex + branchIndex) % 2 == 0) routeDirection else -routeDirection
            val sideOffset = random.nextInt(120, 310).toFloat() * side
            val centerX = (routeCenterAtY + sideOffset).coerceIn(minCenter, maxCenter)

            val branch = Platform(
                x = centerX - width / 2f,
                yMeters = y,
                width = width,
                type = type,
                moveDirection = if (random.nextBoolean()) 1f else -1f,
                safeRoute = false
            )

            val reachableFromPrevious = isReachableJump(fromCenterX, fromY, centerX, y, landingWidth = width)
            val reachableToNext = isReachableJump(centerX, y, toCenterX, toY, landingWidth = width)
            val notTooCloseToRoute = abs(centerX - routeCenterAtY) > 76f
            if (reachableFromPrevious && reachableToNext && notTooCloseToRoute && canPlacePlatform(branch, extraMarginPx = 14f)) {
                platforms += branch
                maybeCreateBoosterOnPlatform(centerX, y, segmentIndex + branchIndex + 500, profile, safeRoute = false)
            }
        }
    }

    private fun chooseChallengePlatformType(): PlatformType {
        val roll = random.nextFloat()
        return when {
            config.rarePlatforms && roll < 0.08f -> PlatformType.Rare
            config.fragilePlatforms && roll < 0.34f -> PlatformType.Fragile
            config.vanishingPlatforms && roll < 0.62f -> PlatformType.Vanishing
            config.movingPlatforms && roll < 0.90f -> PlatformType.Moving
            else -> PlatformType.Static
        }
    }

    private fun createHazardsForSegment(
        fromCenterX: Float,
        toCenterX: Float,
        fromY: Float,
        toY: Float,
        gameWidth: Float,
        segmentIndex: Int,
        profile: LevelGenerationProfile
    ) {
        if (toY < 14f) return
        val attempts = if (random.nextFloat() < profile.hazardPressure) 1 else 0
        repeat(attempts) {
            val fraction = 0.34f + random.nextFloat() * 0.48f
            val y = fromY + (toY - fromY) * fraction
            val routeCenterAtY = fromCenterX + (toCenterX - fromCenterX) * fraction
            val pattern = ((config.level - 1) % 5) + 1
            val forbiddenRadius = (178f - pattern * 11f).coerceAtLeast(118f)
            val side = if ((segmentIndex + it) % 2 == 0) 1f else -1f
            val x = (routeCenterAtY + side * random.nextInt(forbiddenRadius.toInt(), 315).toFloat())
                .coerceIn(48f, gameWidth - 48f)

            val tooCloseToRoute = abs(x - routeCenterAtY) < forbiddenRadius
            if (tooCloseToRoute) return@repeat

            val type = if (random.nextFloat() < 0.56f) HazardType.Monster else HazardType.Spike
            val hazard = Hazard(
                x = x,
                yMeters = y,
                type = type,
                width = if (type == HazardType.Monster) 54f else 70f
            )
            if (canPlaceHazard(hazard)) hazards += hazard
        }
    }

    private fun maybeCreateBoosterOnPlatform(
        centerX: Float,
        yMeters: Float,
        platformIndex: Int,
        profile: LevelGenerationProfile,
        safeRoute: Boolean
    ) {
        // No boosters at all on the first three levels: the player learns the
        // core jumping on a clean field first. From level 4 onwards boosters
        // start appearing, a little more frequently than before.
        if (config.level <= 3) return

        val frequencyBoost = 1.35f
        val baseChance = if (safeRoute) {
            config.boosterChance * 0.62f * frequencyBoost
        } else {
            config.boosterChance * 1.30f * frequencyBoost
        }
        val rhythmBonus = if (platformIndex % profile.forcedRecoveryEvery == 0) 0.05f else 0f
        if (random.nextFloat() > baseChance + rhythmBonus) return
        maybeCreateBooster(centerX, yMeters + 0.72f)
    }

    private fun maxSafeVerticalGapMeters(): Float {
        val theoreticalPeak = baseJumpVelocity * baseJumpVelocity / (2f * abs(gravity))
        return min(safeMaxPlatformGapMeters, theoreticalPeak * 0.77f)
    }

    private fun maxReachableCenterDeltaPx(gapMeters: Float): Float {
        val g = abs(gravity)
        val discriminant = (baseJumpVelocity * baseJumpVelocity - 2f * g * gapMeters).coerceAtLeast(0.01f)
        val descendingTime = (baseJumpVelocity + sqrt(discriminant)) / g

        // 64% of physical reach: hard enough to require real control, but not
        // balanced around perfect accelerometer input or perfect edge-press timing.
        val physicsReach = maxHorizontalSpeed * descendingTime * 0.64f
        return min(safeMaxCenterDeltaPx, physicsReach)
    }

    private fun isReachableJump(
        fromCenterX: Float,
        fromY: Float,
        toCenterX: Float,
        toY: Float,
        landingWidth: Float
    ): Boolean {
        val gap = toY - fromY
        if (gap <= 0f || gap > maxSafeVerticalGapMeters() + 0.03f) return false
        val allowed = maxReachableCenterDeltaPx(gap) + landingWidth * 0.20f
        return abs(toCenterX - fromCenterX) <= allowed
    }

    /**
     * Caps [rawWidth] so a platform can never grow so wide (after [elementScale])
     * that the route stops fitting on screen. The cap leaves at least enough
     * horizontal room for the hero to move between left/right lanes, which keeps
     * every generated level placeable and therefore beatable.
     */
    private fun safePlatformWidth(rawWidth: Float): Float {
        val maxOnScreen = (currentGameWidth - 2f * safeSidePaddingPx) * 0.62f
        return rawWidth.coerceAtMost(maxOnScreen.coerceAtLeast(96f))
    }

    private fun widthForPlatform(type: PlatformType): Float {
        val pattern = ((config.level - 1) % 5) + 1
        val raw = (when (type) {
            PlatformType.Rare -> 88f
            PlatformType.Fragile -> 100f
            PlatformType.Vanishing -> 108f
            PlatformType.Moving -> 124f - pattern * 5f
            PlatformType.Static -> 146f - pattern * 8f
        }.coerceAtLeast(82f)) * elementScale
        return safePlatformWidth(raw)
    }

    private fun canPlacePlatform(candidate: Platform, extraMarginPx: Float): Boolean {
        return platforms.none { platformsOverlap(it, candidate, extraMarginPx) }
    }

    /**
     * On-screen drawn height (px) of a platform of the given type at width
     * [drawWidth]. Mirrors the sprite aspect ratios used in [drawPlatforms] so
     * the overlap test matches what the player actually sees. Falls back to a
     * conservative ratio if the sprite isn't cached yet.
     */
    private fun platformDrawHeight(type: PlatformType, drawWidth: Float): Float {
        val sprite = spriteCache.get(Sprite.forPlatform(type))
        val aspect = if (sprite != null && sprite.width > 0) {
            sprite.height.toFloat() / sprite.width.toFloat()
        } else {
            0.50f
        }
        return drawWidth * aspect
    }

    private fun platformsOverlap(a: Platform, b: Platform, extraMarginPx: Float): Boolean {
        // Two platforms visually overlap only if their drawn footprints intersect
        // both vertically AND horizontally. The vertical threshold is derived
        // from the platforms' actual drawn heights (NOT a tiny fixed constant),
        // otherwise large/scaled sprites placed a little apart still clip into
        // each other on screen.
        val aHeight = platformDrawHeight(a.type, a.width)
        val bHeight = platformDrawHeight(b.type, b.width)
        // Require vertical separation of at least ~46% of the combined drawn
        // heights plus the margin. This is large enough to stop scaled sprites
        // from visually clipping into each other, while still small enough that
        // the guaranteed route platforms (spaced by the minimum jump gap) never
        // reject themselves.
        val verticalThreshold = (aHeight + bHeight) * 0.46f + extraMarginPx
        val verticalPx = abs(a.yMeters - b.yMeters) * metersToPx
        if (verticalPx >= verticalThreshold) return false
        val aLeft = a.x - extraMarginPx
        val aRight = a.x + a.width + extraMarginPx
        val bLeft = b.x - extraMarginPx
        val bRight = b.x + b.width + extraMarginPx
        return aLeft < bRight && aRight > bLeft
    }

    private fun canPlaceHazard(candidate: Hazard): Boolean {
        return hazards.none { hazard ->
            val verticalPx = abs(hazard.yMeters - candidate.yMeters) * metersToPx
            val horizontal = abs(hazard.x - candidate.x)
            verticalPx < 56f && horizontal < (hazard.width + candidate.width) * 0.55f + 20f
        }
    }

    private fun maybeCreateBooster(x: Float, yMeters: Float) {
        // NOTE: the spawn decision (probability + the level 1-3 lockout) is made
        // by the caller (maybeCreateBoosterOnPlatform). This function just picks
        // a type and places the booster, so there is no second probability gate
        // here — a previous double-gate made boosters almost never appear.
        val type = when (random.nextInt(5)) {
            0 -> BoosterType.Spring
            1 -> BoosterType.Jetpack
            2 -> BoosterType.Shield
            3 -> BoosterType.Magnet
            else -> BoosterType.Star
        }
        boosters += Booster(x = x, yMeters = yMeters, type = type)
    }

    private fun sanitizeHazardsAgainstRoute() {
        val route = platforms.filter { it.safeRoute }.sortedBy { it.yMeters }
        if (route.isEmpty()) return

        hazards.removeAll { hazard ->
            val below = route.lastOrNull { it.yMeters <= hazard.yMeters }
            val above = route.firstOrNull { it.yMeters >= hazard.yMeters }
            val routeCenter = when {
                below != null && above != null && above.yMeters != below.yMeters -> {
                    val t = ((hazard.yMeters - below.yMeters) / (above.yMeters - below.yMeters)).coerceIn(0f, 1f)
                    (below.x + below.width / 2f) + ((above.x + above.width / 2f) - (below.x + below.width / 2f)) * t
                }
                below != null -> below.x + below.width / 2f
                above != null -> above.x + above.width / 2f
                else -> return@removeAll false
            }
            val pattern = ((config.level - 1) % 5) + 1
            val forbiddenRadius = (126f - pattern * 7f).coerceAtLeast(90f)
            abs(hazard.x - routeCenter) < forbiddenRadius
        }
    }

    /**
     * Removes any hazard whose drawn footprint overlaps the drawn footprint of
     * ANY platform (route platforms AND optional branches), within a vertical
     * band around the hazard. This guarantees enemies never appear sitting on
     * top of, or visually clipping into, a platform — which the route-centre
     * check alone did not catch for branch/wide platforms.
     *
     * Footprints are computed from the same draw sizes used in onDraw, so the
     * test matches what the player actually sees.
     */
    private fun sanitizeHazardsAgainstPlatforms() {
        if (platforms.isEmpty()) return
        // Vertical proximity (in meters) within which a hazard and a platform are
        // considered to share screen space. Derived from the larger of the two
        // drawn heights so it scales with elementScale.
        val verticalBandMeters = (hazardDrawSize * 0.5f + 40f) / metersToPx

        hazards.removeAll { hazard ->
            val hazardHalf = hazardDrawSize * 0.42f
            val hazardLeft = hazard.x - hazardHalf
            val hazardRight = hazard.x + hazardHalf

            platforms.any { platform ->
                if (abs(platform.yMeters - hazard.yMeters) > verticalBandMeters) return@any false
                // Horizontal overlap of the platform's solid top vs the hazard,
                // with a small margin so they never even touch.
                val margin = 12f
                val platformLeft = platform.x - margin
                val platformRight = platform.x + platform.width + margin
                hazardLeft < platformRight && hazardRight > platformLeft
            }
        }
    }

    /**
     * Occasionally spawns a SINGLE collectible coin just above a platform's
     * surface, centred on the platform. Coins are a rare reward pickup: they
     * never block the route and don't affect passability.
     */
    private fun maybeCreateCoinsOnPlatform(centerX: Float, platformYMeters: Float, platformIndex: Int) {
        // Coins are intentionally rare: only ~15% of platforms (and never the
        // first couple) carry one, so finding a coin feels like a small reward
        // rather than a constant stream.
        if (platformIndex < 2) return
        if (random.nextFloat() > 0.15f) return

        // Always a single coin, hovering a little above the platform top so the
        // hero scoops it up on the way through.
        val yMeters = platformYMeters + 0.55f
        val x = centerX.coerceIn(coinDrawSize, currentGameWidth - coinDrawSize)
        coins += Coin(x = x, yMeters = yMeters)
    }

    private fun validateGeneratedWorld(): Boolean {
        val route = platforms.filter { it.safeRoute }.sortedBy { it.yMeters }
        if (route.isEmpty()) return false

        for (i in 1 until route.size) {
            val previous = route[i - 1]
            val current = route[i]
            val previousCenter = previous.x + previous.width / 2f
            val currentCenter = current.x + current.width / 2f
            val gap = current.yMeters - previous.yMeters
            if (!isReachableJump(previousCenter, previous.yMeters, currentCenter, current.yMeters, current.width)) {
                return false
            }
        }

        for (i in platforms.indices) {
            for (j in i + 1 until platforms.size) {
                if (platformsOverlap(platforms[i], platforms[j], extraMarginPx = 2f)) return false
            }
        }

        return route.last().yMeters >= config.targetHeightMeters
    }

    @Suppress("unused")
    private fun validateGeneratedWorldOrThrow() {
        // Kept only for compatibility with older milestones. Production gameplay
        // must never crash because of a generated level; bad content is filtered
        // or regenerated in resetWorld().
        validateGeneratedWorld()
    }

    private fun update(dt: Float) {
        previousHeroY = heroY

        val controlDirection = when {
            abs(manualDirection) > 0.05f -> manualDirection
            abs(accelerometerDirection) > 0.08f -> accelerometerDirection
            else -> 0f
        }

        heroX += controlDirection * maxHorizontalSpeed * dt
        if (heroX < -heroRadius) heroX = width + heroRadius
        if (heroX > width + heroRadius) heroX = -heroRadius

        if (jetpackLeft > 0f) {
            jetpackLeft -= dt
            heroVelocityY = 11.5f
        } else {
            heroVelocityY += gravity * dt
        }
        if (magnetLeft > 0f) magnetLeft -= dt

        heroY += heroVelocityY * dt

        moveDynamicObjects(dt)
        handlePlatformCollisions()
        handleBoosterCollisions()
        handleCoinCollisions()
        handleHazardCollisions()

        val visibleMeters = max(8f, height / metersToPx)
        val desiredCamera = heroY - visibleMeters * 0.55f
        cameraY = max(cameraY + config.scrollSpeed * dt, max(cameraY, desiredCamera))

        highestMeters = max(highestMeters, heroY.toInt().coerceAtLeast(0))
        score = highestMeters + bonusScore

        sendHud(force = false)

        if (highestMeters >= config.targetHeightMeters) {
            finishGame(passed = true)
            return
        }

        val heroScreenY = worldToScreenY(heroY)
        if (heroScreenY > height + 150f) {
            finishGame(passed = false)
        }
    }

    private fun moveDynamicObjects(dt: Float) {
        platforms.forEach { platform ->
            if (platform.type == PlatformType.Moving || platform.type == PlatformType.Rare) {
                val speed = if (platform.type == PlatformType.Rare) 145f else 95f
                platform.x += platform.moveDirection * speed * dt
                if (platform.x < 18f) {
                    platform.x = 18f
                    platform.moveDirection = 1f
                }
                if (platform.x + platform.width > width - 18f) {
                    platform.x = width - 18f - platform.width
                    platform.moveDirection = -1f
                }
            }
        }

        hazards.forEach { hazard ->
            if (hazard.type == HazardType.Monster) {
                hazard.x += kotlin.math.sin((System.nanoTime() / 280_000_000.0)).toFloat() * 1.8f
            }
        }
    }

    private fun handlePlatformCollisions() {
        if (heroVelocityY > 0f) return
        val heroBottom = heroY
        val heroLeft = heroX - heroRadius
        val heroRight = heroX + heroRadius

        platforms.forEach { platform ->
            if (!platform.visible) return@forEach
            val crossedPlatform = previousHeroY >= platform.yMeters && heroBottom <= platform.yMeters
            val overlapsX = heroRight >= platform.x && heroLeft <= platform.x + platform.width
            if (crossedPlatform && overlapsX) {
                heroY = platform.yMeters
                heroVelocityY = when (platform.type) {
                    PlatformType.Rare -> baseJumpVelocity * 1.42f
                    else -> baseJumpVelocity
                }

                jumpCount += 1
                callback?.onJumpSound()

                if (!platform.touched) {
                    platform.touched = true
                    when (platform.type) {
                        PlatformType.Vanishing -> platform.visible = false
                        PlatformType.Fragile -> platform.visible = false
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun handleBoosterCollisions() {
        boosters.forEach { booster ->
            if (booster.collected) return@forEach
            val screenY = worldToScreenY(booster.yMeters)
            val distanceX = abs(heroX - booster.x)
            val distanceY = abs(worldToScreenY(heroY + heroHeightMeters / 2f) - screenY)
            val magnetPull = magnetLeft > 0f && distanceX < 180f && distanceY < 180f
            if (magnetPull) {
                booster.x += (heroX - booster.x) * 0.12f
            }
            if (distanceX < 46f * elementScale && distanceY < 58f * elementScale) {
                booster.collected = true
                callback?.onBoosterSound()
                bonusScore += when (booster.type) {
                    BoosterType.Star -> 100
                    else -> 30
                }
                when (booster.type) {
                    BoosterType.Spring -> {
                        springUses += 1
                        heroVelocityY = baseJumpVelocity * 1.85f
                    }
                    BoosterType.Jetpack -> {
                        jetpackUses += 1
                        jetpackLeft = 5f
                    }
                    BoosterType.Shield -> shieldLives = min(3, shieldLives + 1)
                    BoosterType.Magnet -> magnetLeft = 7f
                    BoosterType.Star -> starsCollected += 1
                }
            }
        }
    }

    private fun handleCoinCollisions() {
        coins.forEach { coin ->
            if (coin.collected) return@forEach
            val screenY = worldToScreenY(coin.yMeters)
            val distanceX = abs(heroX - coin.x)
            val distanceY = abs(worldToScreenY(heroY + heroHeightMeters / 2f) - screenY)
            // Coins are pulled in by an active magnet, just like boosters.
            val magnetPull = magnetLeft > 0f && distanceX < 180f && distanceY < 180f
            if (magnetPull) {
                coin.x += (heroX - coin.x) * 0.14f
            }
            val pickupX = (coinDrawSize * 0.5f + heroRadius * 0.6f)
            val pickupY = (coinDrawSize * 0.5f + heroRadius * 0.6f)
            if (distanceX < pickupX && distanceY < pickupY) {
                coin.collected = true
                coinsCollected += 1
                bonusScore += 10
                callback?.onBoosterSound()
            }
        }
    }

    private fun handleHazardCollisions() {
        hazards.forEach { hazard ->
            if (!hazard.active) return@forEach
            val heroCenterY = worldToScreenY(heroY + heroHeightMeters / 2f)
            val hazardY = worldToScreenY(hazard.yMeters)
            val hit = abs(heroX - hazard.x) < 45f * elementScale && abs(heroCenterY - hazardY) < 48f * elementScale
            if (!hit) return@forEach

            val jumpedOnMonster = hazard.type == HazardType.Monster && heroVelocityY < 0f && heroY > hazard.yMeters
            if (jumpedOnMonster) {
                hazard.active = false
                heroVelocityY = baseJumpVelocity * 1.15f
                bonusScore += 20
                monsterKills += 1
                callback?.onMonsterStompSound()
            } else if (shieldLives > 0) {
                shieldLives -= 1
                hazardHits += 1
                hazard.active = false
                heroVelocityY = baseJumpVelocity * 0.9f
                callback?.onHazardSound()
            } else {
                hazardHits += 1
                callback?.onHazardSound()
                finishGame(passed = false)
            }
        }
    }

    private fun finishGame(passed: Boolean) {
        if (isFinished) return
        isFinished = true
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
        val baseReward = if (passed) {
            when (((config.level - 1) % 5) + 1) {
                1 -> 50
                2 -> 100
                3 -> 200
                4 -> 200
                else -> 300
            }
        } else 0
        // Coins picked up during the run pay out on top of the base reward (5
        // reward coins each). On a failed run the base reward is 0 but the
        // player still keeps the coins they actually collected.
        val rewardCoins = baseReward + coinsCollected * 5
        callback?.onGameFinished(
            JumperGameResult(
                level = config.level,
                passed = passed,
                score = score,
                heightMeters = highestMeters,
                rewardCoins = rewardCoins,
                jumpCount = jumpCount,
                springUses = springUses,
                jetpackUses = jetpackUses,
                monsterKills = monsterKills,
                starsCollected = starsCollected,
                hazardHits = hazardHits,
                levelPattern = ((config.level - 1) % 5) + 1
            )
        )
    }

    private fun sendHud(force: Boolean) {
        if (!force && score == lastHudScore && highestMeters == lastHudHeight && shieldLives == lastHudShield) return
        lastHudScore = score
        lastHudHeight = highestMeters
        lastHudShield = shieldLives
        callback?.onHudChanged(
            level = config.level,
            heightMeters = highestMeters,
            targetMeters = config.targetHeightMeters,
            score = score,
            shieldLives = shieldLives
        )
    }

    private fun drawBackground(canvas: Canvas) {
        val bg = backgroundBitmap
        if (bg != null) {
            // Draw the selected background image, scaled to fully cover the
            // view (centre-crop) so there are never empty/white edges
            // regardless of the image's aspect ratio.
            drawBackgroundImage(canvas, bg)
        } else {
            // No dedicated image for this background -> procedural gradient.
            bgPaint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                theme.backgroundTop,
                theme.backgroundBottom,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            bgPaint.shader = null

            // Clouds belong to the gradient look only; an image background
            // already contains its own scenery.
            cloudPaint.color = theme.cloudColor
            val offset = (cameraY * 22f) % 260f
            for (i in -1..6) {
                val y = i * 180f + offset
                canvas.drawOval(RectF(35f, y, 220f, y + 62f), cloudPaint)
                canvas.drawOval(RectF(width - 250f, y + 80f, width - 42f, y + 142f), cloudPaint)
            }
        }
    }

    /**
     * Draws [bg] covering the whole view with a centre-crop fit: the image is
     * scaled by the larger of the width/height ratios and centred, so it
     * always fills the screen with no empty borders.
     */
    private fun drawBackgroundImage(canvas: Canvas, bg: android.graphics.Bitmap) {
        if (width <= 0 || height <= 0) return
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val scale = maxOf(viewW / bg.width, viewH / bg.height)
        val drawW = bg.width * scale
        val drawH = bg.height * scale
        val left = (viewW - drawW) / 2f
        val top = (viewH - drawH) / 2f
        backgroundSrcRect.set(0, 0, bg.width, bg.height)
        backgroundDstRect.set(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bg, backgroundSrcRect, backgroundDstRect, null)
    }

    private fun drawGoalLine(canvas: Canvas) {
        val goalY = worldToScreenY(config.targetHeightMeters.toFloat())
        if (goalY < -60f || goalY > height + 60f) return
        goalPaint.color = theme.goalColor
        goalPaint.strokeWidth = 7f
        canvas.drawLine(0f, goalY, width.toFloat(), goalY, goalPaint)
        textPaint.color = theme.canvasTextColor
        canvas.drawText("GOAL", width / 2f, goalY - 12f, textPaint)
    }

    /**
     * Draws all visible platforms from their artwork sprite.
     * The sprite is stretched to the platform's logical width; its height is
     * derived from the sprite aspect ratio so the art is never distorted.
     * The platform's collision top stays at screen-y `y` (the sprite extends
     * slightly above/below for the glow, which does not affect physics).
     */
    private fun drawPlatforms(canvas: Canvas) {
        platforms.forEach { platform ->
            if (!platform.visible) return@forEach
            val y = worldToScreenY(platform.yMeters)
            if (y < -160f || y > height + 160f) return@forEach

            val sprite = spriteCache.get(Sprite.forPlatform(platform.type)) ?: return@forEach
            val drawWidth = platform.width
            val aspect = sprite.height.toFloat() / sprite.width.toFloat()
            val drawHeight = drawWidth * aspect

            // Anchor: the platform's solid surface sits near the sprite's
            // upper third, so the glow/base of the art hangs below `y`.
            val left = platform.x
            val top = y - drawHeight * 0.30f
            spriteDstRect.set(left, top, left + drawWidth, top + drawHeight)
            spriteSrcRect.set(0, 0, sprite.width, sprite.height)
            canvas.drawBitmap(sprite, spriteSrcRect, spriteDstRect, spritePaint)
        }
    }

    /** Draws all uncollected boosters from their artwork sprite. */
    private fun drawBoosters(canvas: Canvas) {
        boosters.forEach { booster ->
            if (booster.collected) return@forEach
            val y = worldToScreenY(booster.yMeters)
            if (y < -120f || y > height + 120f) return@forEach

            val sprite = Sprite.forBooster(booster.type)?.let { spriteCache.get(it) }
            if (sprite != null) {
                drawSpriteCentered(canvas, sprite, booster.x, y, boosterDrawSize)
            } else {
                // No dedicated art (Magnet / Star): draw a distinct coloured
                // badge so it can never be confused with the shield pickup.
                drawBoosterBadge(canvas, booster.type, booster.x, y)
            }
        }
    }

    /** Distinct fallback badge for boosters without sprite art. */
    private fun drawBoosterBadge(canvas: Canvas, type: BoosterType, cx: Float, cy: Float) {
        val radius = boosterDrawSize * 0.42f
        val (fillColor, label) = when (type) {
            BoosterType.Magnet -> Color.rgb(229, 57, 53) to "M"
            BoosterType.Star -> Color.rgb(255, 202, 40) to "★"
            else -> Color.rgb(120, 144, 156) to "?"
        }
        bgPaint.color = fillColor
        canvas.drawCircle(cx, cy, radius, bgPaint)
        bgPaint.color = Color.argb(60, 255, 255, 255)
        canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.3f, radius * 0.45f, bgPaint)
        textPaint.color = Color.WHITE
        canvas.drawText(label, cx, cy + radius * 0.36f, textPaint)
    }

    /** Draws all uncollected in-game coins from the coin sprite. */
    private fun drawCoins(canvas: Canvas) {
        val sprite = spriteCache.get(Sprite.CoinPickup)
        coins.forEach { coin ->
            if (coin.collected) return@forEach
            val y = worldToScreenY(coin.yMeters)
            if (y < -120f || y > height + 120f) return@forEach
            if (sprite != null) {
                drawSpriteCentered(canvas, sprite, coin.x, y, coinDrawSize)
            } else {
                // Fallback: a simple gold disc if the coin art is ever missing.
                bgPaint.color = Color.rgb(255, 202, 40)
                canvas.drawCircle(coin.x, y, coinDrawSize * 0.42f, bgPaint)
                bgPaint.color = Color.rgb(255, 235, 130)
                canvas.drawCircle(coin.x, y, coinDrawSize * 0.27f, bgPaint)
            }
        }
    }

    /** Draws all active hazards from their artwork sprite. */
    private fun drawHazards(canvas: Canvas) {
        hazards.forEach { hazard ->
            if (!hazard.active) return@forEach
            val y = worldToScreenY(hazard.yMeters)
            if (y < -150f || y > height + 150f) return@forEach

            val sprite = spriteCache.get(Sprite.forHazard(hazard.type)) ?: return@forEach
            drawSpriteCentered(canvas, sprite, hazard.x, y, hazardDrawSize)
        }
    }

    /**
     * Draws [sprite] centred at ([cx], [cy]) so that its larger side equals
     * [targetSize], preserving the sprite's aspect ratio.
     */
    private fun drawSpriteCentered(
        canvas: Canvas,
        sprite: android.graphics.Bitmap,
        cx: Float,
        cy: Float,
        targetSize: Float
    ) {
        val sw = sprite.width.toFloat()
        val sh = sprite.height.toFloat()
        val scale = targetSize / maxOf(sw, sh)
        val halfW = sw * scale / 2f
        val halfH = sh * scale / 2f
        spriteSrcRect.set(0, 0, sprite.width, sprite.height)
        spriteDstRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        canvas.drawBitmap(sprite, spriteSrcRect, spriteDstRect, spritePaint)
    }

    private fun drawHero(canvas: Canvas) {
        val heroScreenY = worldToScreenY(heroY + heroHeightMeters / 2f)

        // The full hero look comes from JumperSkinRenderer + the active skin.
        skinRenderer.draw(canvas, skin, heroX, heroScreenY, heroRadius)

        if (shieldLives > 0) {
            shieldPaint.color = Color.argb(110, 77, 208, 225)
            canvas.drawCircle(heroX, heroScreenY, heroRadius + 12f, shieldPaint)
        }
    }

    private fun worldToScreenY(worldMeters: Float): Float {
        return height - 96f - (worldMeters - cameraY) * metersToPx
    }
}
