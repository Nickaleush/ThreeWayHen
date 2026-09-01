package threeway.henroute.orchard.feature.game

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.databinding.FragmentGameBinding
import threeway.henroute.orchard.games.hen.HenGameResult
import threeway.henroute.orchard.games.hen.HenGameView
import threeway.henroute.orchard.games.hen.HenHudState
import threeway.henroute.orchard.games.hen.HenLevelConfig

class GameFragment : SafeAreaFragment(R.layout.fragment_game), HenGameView.Callback {

    override val protectSystemGestures: Boolean = true

    private var _binding: FragmentGameBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: GameViewModel by viewModels {
        val app = requireActivity().application as App
        GameViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    private val soundManager: SoundManager
        get() = (requireActivity().application as App).serviceLocator.soundManager

    private var level = 1
    private var hasFinished = false
    private var isPausedByUser = false
    private var levelStarted = false
    private var levelDurationSeconds = 60

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGameBinding.bind(view)
        level = requireArguments().getInt(ARG_LEVEL, 1).coerceIn(1, 40)
        levelDurationSeconds = HenLevelConfig.forLevel(level).durationSeconds

        binding.gameView.callback = this
        binding.gameView.isInvisible = true
        binding.levelTextView.text = getString(R.string.game_level_format, level)
        setupListeners()
        hidePausePanel(resumeGame = false)
        observeProfile()
        viewModel.loadPlayerProfile()
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profile.collect { profile ->
                        if (_binding == null || !viewModel.profileLoaded.value) return@collect
                        binding.gameView.setAppearance(profile.selectedSkinId)
                    }
                }
                launch {
                    viewModel.profileLoaded.collect { loaded ->
                        if (!loaded || levelStarted || _binding == null) return@collect
                        val profile = viewModel.profile.value
                        binding.gameView.setAppearance(profile.selectedSkinId)
                        binding.gameView.isInvisible = false
                        levelStarted = true
                        binding.gameView.post {
                            if (_binding != null) {
                                binding.gameView.startLevel(level)
                                soundManager.startRunLoop()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.pauseButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            showPausePanel()
        }
        binding.continueButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            hidePausePanel(resumeGame = true)
        }
        binding.replayButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            hasFinished = false
            hidePausePanel(resumeGame = false)
            binding.gameView.restart()
            soundManager.playMusic(SoundManager.MusicTrack.Game)
            soundManager.startRunLoop()
        }
        binding.homeButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            soundManager.stopRunLoop()
            binding.gameView.stopGameLoop()
            findNavController().popBackStack(R.id.menuFragment, false)
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager.playMusic(SoundManager.MusicTrack.Game)
        if (_binding != null && levelStarted && !isPausedByUser && !hasFinished) {
            binding.gameView.setPaused(false)
            soundManager.startRunLoop()
        }
    }

    override fun onPause() {
        soundManager.stopRunLoop()
        if (_binding != null && !hasFinished) binding.gameView.setPaused(true)
        super.onPause()
    }

    override fun onDestroyView() {
        soundManager.stopRunLoop()
        binding.gameView.callback = null
        binding.gameView.stopGameLoop()
        _binding = null
        super.onDestroyView()
    }

    override fun onHudChanged(state: HenHudState) {
        if (_binding == null) return
        binding.levelTextView.text = getString(R.string.game_level_format, state.level)
        binding.distanceTextView.text = getString(R.string.game_distance_format, state.distance)
        binding.feathersTextView.text = getString(R.string.game_feathers_format, state.feathers)
        binding.coinsTextView.text = getString(R.string.game_coins_format, state.coins)
        binding.livesTextView.text = getString(R.string.game_lives_format, state.lives)
        binding.heartOne.isVisible = state.lives >= 1
        binding.heartTwo.isVisible = state.lives >= 2
        binding.heartThree.isVisible = state.lives >= 3
        val elapsed = (levelDurationSeconds - state.timeLeftSeconds).coerceAtLeast(0)
        val progress = ((elapsed * 100f) / levelDurationSeconds.coerceAtLeast(1)).toInt().coerceIn(0, 100)
        binding.distanceProgressBar.progress = progress
        binding.progressPercentTextView.text = "$progress%"
        binding.progressTrackContainer.post {
            if (_binding == null) return@post
            val travel = (binding.progressTrackContainer.width - binding.progressHenImageView.width).coerceAtLeast(0)
            binding.progressHenImageView.translationX = travel * (progress / 100f)
        }
        binding.streakTextView.text = getString(R.string.game_streak_format, state.streak)
        val inactiveAlpha = 0.48f
        binding.laneLeftArrow.alpha = if (state.selectedLane < 0) 1f else inactiveAlpha
        binding.laneCenterArrow.alpha = if (state.selectedLane == 0) 1f else inactiveAlpha
        binding.laneRightArrow.alpha = if (state.selectedLane > 0) 1f else inactiveAlpha
    }

    override fun onJumpSound() = soundManager.playEffect(SoundManager.SoundEffect.Jump)
    override fun onSwipeSound() = soundManager.playEffect(SoundManager.SoundEffect.Swipe)
    override fun onCollectFeather() = soundManager.playEffect(SoundManager.SoundEffect.Feather)
    override fun onCollectCoin() = soundManager.playEffect(SoundManager.SoundEffect.Coin)
    override fun onHitObstacle() = soundManager.playEffect(SoundManager.SoundEffect.Hit)
    override fun onForkAppeared() = soundManager.playEffect(SoundManager.SoundEffect.Fork)

    override fun onGameFinished(result: HenGameResult) {
        if (hasFinished || _binding == null) return
        hasFinished = true
        soundManager.stopRunLoop()
        soundManager.playEffect(
            if (result.passed) SoundManager.SoundEffect.Win else SoundManager.SoundEffect.Lose
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveResult(result)
            if (_binding == null) return@launch
            findNavController().navigate(
                R.id.action_gameFragment_to_resultFragment,
                bundleOf(
                    ARG_LEVEL to result.level,
                    ARG_SCORE to result.score,
                    ARG_PASSED to result.passed,
                    ARG_REWARD_COINS to result.rewardCoins,
                    ARG_DISTANCE to result.distance,
                    ARG_FEATHERS to result.feathersCollected,
                    ARG_COINS_COLLECTED to result.coinsCollected,
                    ARG_LIVES to result.livesRemaining,
                    ARG_BEST_STREAK to result.bestStreak,
                    ARG_SKIN_ID to viewModel.profile.value.selectedSkinId
                )
            )
        }
    }

    private fun showPausePanel() {
        isPausedByUser = true
        binding.gameView.setPaused(true)
        soundManager.stopRunLoop()
        binding.pausePanel.isVisible = true
    }

    private fun hidePausePanel(resumeGame: Boolean) {
        isPausedByUser = false
        binding.pausePanel.isVisible = false
        if (resumeGame && !hasFinished && levelStarted) {
            binding.gameView.setPaused(false)
            soundManager.playMusic(SoundManager.MusicTrack.Game)
            soundManager.startRunLoop()
        }
    }

    companion object {
        const val ARG_LEVEL = "level"
        const val ARG_SCORE = "score"
        const val ARG_PASSED = "passed"
        const val ARG_REWARD_COINS = "rewardCoins"
        const val ARG_DISTANCE = "distance"
        const val ARG_FEATHERS = "feathersCollected"
        const val ARG_COINS_COLLECTED = "coinsCollected"
        const val ARG_LIVES = "livesRemaining"
        const val ARG_BEST_STREAK = "bestStreak"
        const val ARG_SKIN_ID = "skinId"
    }
}
