package com.businessdoomguy.minigamestarter.feature.leaders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.data.db.entity.AchievementEntity
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeadersViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _achievements.value = gameRepository.getAchievements()
        }
    }
}

class LeadersViewModelFactory(
    private val gameRepository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LeadersViewModel(gameRepository) as T
    }
}
