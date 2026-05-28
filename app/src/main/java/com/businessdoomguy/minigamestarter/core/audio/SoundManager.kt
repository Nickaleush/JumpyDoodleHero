package com.businessdoomguy.minigamestarter.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator

/**
 * Central audio controller for Jumper.
 *
 * Responsibilities:
 *  - play short sound effects (jump, booster, hazard, win, lose);
 *  - play looping background music for the menu and the game;
 *  - respect the player's sound / music settings.
 *
 * Asset strategy:
 *  - if the project provides real audio files in `res/raw` with the names
 *    listed in [SoundEffect] / [MusicTrack], they are loaded and used;
 *  - if those files are missing (current placeholder build), the manager
 *    falls back to short synthesized tones via [ToneGenerator] so the audio
 *    settings are still audible and verifiable on a device.
 *
 * Drop real `.ogg` / `.wav` files into `app/src/main/res/raw` using the same
 * resource names and no code change is required.
 *
 * The instance is owned by [com.businessdoomguy.minigamestarter.core.AppServiceLocator]
 * and lives for the whole application process.
 */
class SoundManager(
    private val context: Context
) {

    enum class SoundEffect(val rawResName: String, private val toneType: Int) {
        Jump("sfx_jump", ToneGenerator.TONE_PROP_BEEP),
        Booster("sfx_booster", ToneGenerator.TONE_PROP_ACK),
        MonsterStomp("sfx_monster", ToneGenerator.TONE_PROP_PROMPT),
        Hazard("sfx_hazard", ToneGenerator.TONE_CDMA_LOW_L),
        Win("sfx_win", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD),
        Lose("sfx_lose", ToneGenerator.TONE_CDMA_ABBR_ALERT);

        fun toneType(): Int = toneType
    }

    enum class MusicTrack(val rawResName: String) {
        Menu("music_menu"),
        Game("music_game"),
        Levels("music_levels")
    }

    private var soundEnabled = true
    private var musicEnabled = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** Loaded SoundPool ids. Missing entries mean we fall back to tones. */
    private val effectIds = mutableMapOf<SoundEffect, Int>()

    private var toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    }.getOrNull()

    private var musicPlayer: MediaPlayer? = null

    /** Track the player *wants* (set by playMusic, kept across pause). */
    private var currentTrack: MusicTrack? = null

    /** Track currently loaded into [musicPlayer], or null if none loaded. */
    private var loadedTrack: MusicTrack? = null

    init {
        SoundEffect.entries.forEach { effect ->
            val resId = rawResId(effect.rawResName)
            if (resId != 0) {
                effectIds[effect] = soundPool.load(context, resId, 1)
            }
        }
    }

    /** Applies the latest values from the player profile. */
    fun applySettings(soundEnabled: Boolean, musicEnabled: Boolean) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled
        if (!musicEnabled) {
            pauseMusic()
        } else {
            currentTrack?.let { resumeOrStart(it) }
        }
    }

    fun playEffect(effect: SoundEffect) {
        if (!soundEnabled) return
        val soundId = effectIds[effect]
        if (soundId != null) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            // Placeholder build: no audio asset, use a short synthesized tone.
            runCatching { toneGenerator?.startTone(effect.toneType(), 160) }
        }
    }

    /** Starts (or switches to) a looping background track if music is enabled. */
    fun playMusic(track: MusicTrack) {
        currentTrack = track
        if (!musicEnabled) return
        resumeOrStart(track)
    }

    private fun resumeOrStart(track: MusicTrack) {
        val resId = rawResId(track.rawResName)
        if (resId == 0) {
            // No music asset in this build. Nothing to loop; effects still work.
            return
        }
        // Same track already loaded: just resume if it was paused.
        if (musicPlayer != null && loadedTrack == track) {
            runCatching { if (musicPlayer?.isPlaying == false) musicPlayer?.start() }
            return
        }
        // Different track (or nothing loaded): rebuild the player.
        releaseMusicPlayer()
        musicPlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(0.6f, 0.6f)
            runCatching { start() }
        }
        loadedTrack = if (musicPlayer != null) track else null
    }

    fun pauseMusic() {
        runCatching { if (musicPlayer?.isPlaying == true) musicPlayer?.pause() }
    }

    /**
     * Resumes the last requested track after the app returns to the
     * foreground. Does nothing if music is disabled or no track was ever
     * requested. Used by the Activity's onStart.
     */
    fun resumeMusic() {
        if (!musicEnabled) return
        currentTrack?.let { resumeOrStart(it) }
    }

    fun stopMusic() {
        currentTrack = null
        releaseMusicPlayer()
    }

    private fun releaseMusicPlayer() {
        runCatching {
            musicPlayer?.stop()
            musicPlayer?.release()
        }
        musicPlayer = null
        loadedTrack = null
    }

    /** Releases all audio resources. Call from Application teardown if needed. */
    fun release() {
        releaseMusicPlayer()
        runCatching { soundPool.release() }
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private fun rawResId(name: String): Int {
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }
}
