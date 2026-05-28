package com.businessdoomguy.minigamestarter.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.data.db.entity.PlayerProfileEntity
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val loaded = gameRepository.getProfile()
            _profile.value = loaded
            applyToAudio(loaded)
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch {
            gameRepository.setSoundEnabled(enabled)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    fun setMusic(enabled: Boolean) {
        viewModelScope.launch {
            gameRepository.setMusicEnabled(enabled)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    private fun applyToAudio(profile: PlayerProfileEntity) {
        soundManager.applySettings(
            soundEnabled = profile.soundEnabled,
            musicEnabled = profile.musicEnabled
        )
    }
}

class SettingsViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(gameRepository, soundManager) as T
    }
}
