package threeway.henroute.orchard.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.data.repository.GameRepository

class MenuViewModel(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {
    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()
    private val _selectedSkinId = MutableStateFlow("hen_classic")
    val selectedSkinId: StateFlow<String> = _selectedSkinId.asStateFlow()

    fun loadMenuData() {
        viewModelScope.launch {
            val profile = gameRepository.getProfile()
            _coins.value = profile.coins
            _selectedSkinId.value = profile.selectedSkinId
            soundManager.applySettings(
                profile.soundEnabled,
                profile.musicEnabled,
                profile.soundVolume / 100f,
                profile.musicVolume / 100f
            )
        }
    }
}

class MenuViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MenuViewModel(gameRepository, soundManager) as T
}
