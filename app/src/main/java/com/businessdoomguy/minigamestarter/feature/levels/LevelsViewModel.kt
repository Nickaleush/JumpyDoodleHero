package com.businessdoomguy.minigamestarter.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LevelsViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _maxUnlockedLevel = MutableStateFlow(1)

    /** Highest level the player is currently allowed to start. */
    val maxUnlockedLevel: StateFlow<Int> = _maxUnlockedLevel.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _maxUnlockedLevel.value = gameRepository.getMaxUnlockedLevel()
        }
    }
}

class LevelsViewModelFactory(
    private val gameRepository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LevelsViewModel(gameRepository) as T
    }
}
