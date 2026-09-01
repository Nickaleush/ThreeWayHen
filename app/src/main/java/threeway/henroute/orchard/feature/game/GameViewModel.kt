package threeway.henroute.orchard.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.data.db.entity.PlayerProfileEntity
import threeway.henroute.orchard.data.repository.GameRepository
import threeway.henroute.orchard.games.hen.HenGameResult

class GameViewModel(
    private val gameRepository: GameRepository,
    val soundManager: SoundManager
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    fun loadPlayerProfile() {
        viewModelScope.launch {
            val loaded = gameRepository.getProfile()
            _profile.value = loaded
            soundManager.applySettings(
                soundEnabled = loaded.soundEnabled,
                musicEnabled = loaded.musicEnabled,
                soundVolume = loaded.soundVolume / 100f,
                musicVolume = loaded.musicVolume / 100f
            )
            _profileLoaded.value = true
        }
    }

    suspend fun saveResult(result: HenGameResult) {
        gameRepository.saveHenResult(result)
    }
}

class GameViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(gameRepository, soundManager) as T
    }
}
