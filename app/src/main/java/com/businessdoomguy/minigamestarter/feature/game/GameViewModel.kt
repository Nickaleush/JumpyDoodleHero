package com.businessdoomguy.minigamestarter.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.data.db.entity.PlayerProfileEntity
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import com.businessdoomguy.minigamestarter.games.jumper.JumperGameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameRepository: GameRepository,
    val soundManager: SoundManager
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    /**
     * Profile-loaded signal as a dedicated flow.
     *
     * It starts at false and is set to true exactly once, after the real
     * profile has been read from the database. Using a separate boolean flow
     * (instead of a plain field + relying on a second emission of [profile])
     * is important: StateFlow de-duplicates equal values, so if the loaded
     * profile equals the initial default profile (common for a brand-new
     * player), [profile] would NOT emit a second time and the level would
     * never start. The false -> true transition here always emits.
     */
    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    fun loadPlayerProfile() {
        viewModelScope.launch {
            val loaded = gameRepository.getProfile()
            _profile.value = loaded
            // Keep the audio engine in sync with the player's saved preferences.
            soundManager.applySettings(
                soundEnabled = loaded.soundEnabled,
                musicEnabled = loaded.musicEnabled
            )
            // Emit readiness AFTER the profile so that, by the time the level
            // starts, setAppearance has already been applied (no default-skin
            // flash). false -> true is never de-duplicated.
            _profileLoaded.value = true
        }
    }

    suspend fun saveResult(result: JumperGameResult) {
        gameRepository.saveJumperResult(result)
    }
}

class GameViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(gameRepository, soundManager) as T
    }
}