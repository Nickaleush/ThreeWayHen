package threeway.henroute.orchard.feature.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.data.db.entity.InventoryItemEntity
import threeway.henroute.orchard.data.repository.GameRepository

data class ShopUiState(val coins: Int = 0, val items: List<InventoryItemEntity> = emptyList())

class ShopViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<GameRepository.PurchaseResult>()
    val events: SharedFlow<GameRepository.PurchaseResult> = _events.asSharedFlow()

    fun load() {
        viewModelScope.launch { _state.value = ShopUiState(repository.getCoins(), repository.getShopItems()) }
    }

    fun buyOrSelect(id: String) {
        viewModelScope.launch {
            val result = repository.buyOrSelectItem(id)
            _events.emit(result)
            load()
        }
    }
}

class ShopViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ShopViewModel(repository) as T
}
