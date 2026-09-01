package threeway.henroute.orchard.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.data.db.entity.AchievementEntity
import threeway.henroute.orchard.data.repository.GameRepository

class AchievementsViewModel(private val repository: GameRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val items: StateFlow<List<AchievementEntity>> = _items.asStateFlow()
    fun load() { viewModelScope.launch { _items.value = repository.getAchievements() } }
}

class AchievementsViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AchievementsViewModel(repository) as T
}
