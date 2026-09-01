package threeway.henroute.orchard.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.data.repository.GameRepository

data class LevelsUiState(val maxUnlocked: Int = 1, val highestPassed: Int = 0, val coins: Int = 0)

class LevelsViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(LevelsUiState())
    val state: StateFlow<LevelsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = LevelsUiState(
                maxUnlocked = repository.getMaxUnlockedLevel(),
                highestPassed = repository.getStats().highestLevelPassed,
                coins = repository.getCoins()
            )
        }
    }
}

class LevelsViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = LevelsViewModel(repository) as T
}
