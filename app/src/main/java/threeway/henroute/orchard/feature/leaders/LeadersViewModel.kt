package threeway.henroute.orchard.feature.leaders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.data.db.entity.GameStatsEntity
import threeway.henroute.orchard.data.db.entity.ScoreEntity
import threeway.henroute.orchard.data.repository.GameRepository

data class LeadersUiState(val scores: List<ScoreEntity> = emptyList(), val stats: GameStatsEntity = GameStatsEntity())

class LeadersViewModel(private val repository: GameRepository) : ViewModel() {
    private val _state = MutableStateFlow(LeadersUiState())
    val state: StateFlow<LeadersUiState> = _state.asStateFlow()
    fun load() { viewModelScope.launch { _state.value = LeadersUiState(repository.getTopScores(), repository.getStats()) } }
}

class LeadersViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = LeadersViewModel(repository) as T
}
