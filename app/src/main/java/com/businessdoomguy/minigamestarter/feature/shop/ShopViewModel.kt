package com.businessdoomguy.minigamestarter.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.businessdoomguy.minigamestarter.data.db.entity.InventoryItemEntity
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<InventoryItemEntity>>(emptyList())
    val items: StateFlow<List<InventoryItemEntity>> = _items.asStateFlow()

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun load() {
        viewModelScope.launch {
            _coins.value = gameRepository.getCoins()
            _items.value = gameRepository.getShopItems()
        }
    }

    fun buyOrSelect(itemId: String) {
        viewModelScope.launch {
            when (gameRepository.buyOrSelectItem(itemId)) {
                GameRepository.PurchaseResult.Success -> _messages.emit("Selected")
                GameRepository.PurchaseResult.NotEnoughCoins -> _messages.emit("Not enough coins")
                GameRepository.PurchaseResult.NotFound -> _messages.emit("Item not found")
            }
            load()
        }
    }
}

class ShopViewModelFactory(
    private val gameRepository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShopViewModel(gameRepository) as T
    }
}
