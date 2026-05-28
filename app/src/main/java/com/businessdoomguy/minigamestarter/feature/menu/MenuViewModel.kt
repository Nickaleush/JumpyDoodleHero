package com.businessdoomguy.minigamestarter.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _bestScore = MutableStateFlow(0)
    val bestScore: StateFlow<Int> = _bestScore.asStateFlow()

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    fun loadMenuData() {
        viewModelScope.launch {
            val profile = gameRepository.getProfile()
            _bestScore.value = gameRepository.getBestScore(GameRepository.GAME_ID)
            _coins.value = profile.coins
            // Sync the audio engine with stored preferences before the menu
            // tries to start its background music.
            soundManager.applySettings(
                soundEnabled = profile.soundEnabled,
                musicEnabled = profile.musicEnabled
            )
        }
    }
}

class MenuViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MenuViewModel(gameRepository, soundManager) as T
    }
}
