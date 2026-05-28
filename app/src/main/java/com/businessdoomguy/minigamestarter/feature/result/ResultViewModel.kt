package com.businessdoomguy.minigamestarter.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _bestScore = MutableStateFlow(0)
    val bestScore: StateFlow<Int> = _bestScore.asStateFlow()

    fun loadBestScore() {
        viewModelScope.launch {
            _bestScore.value = gameRepository.getBestScore("jumper")
        }
    }
}

class ResultViewModelFactory(
    private val gameRepository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ResultViewModel(gameRepository) as T
    }
}
