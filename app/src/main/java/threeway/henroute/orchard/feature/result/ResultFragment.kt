package threeway.henroute.orchard.feature.result

import android.os.Bundle
import android.view.View
import android.view.Gravity
import androidx.core.os.bundleOf
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
import threeway.henroute.orchard.databinding.FragmentResultBinding
import threeway.henroute.orchard.feature.game.GameFragment
import threeway.henroute.orchard.feature.shop.ShopArtwork
import threeway.henroute.orchard.games.hen.HenLevelConfig

class ResultFragment : SafeAreaFragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ResultViewModel by viewModels {
        val app = requireActivity().application as App
        ResultViewModelFactory(app.serviceLocator.gameRepository)
    }

    private val soundManager: SoundManager
        get() = (requireActivity().application as App).serviceLocator.soundManager

    private var score = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val arguments = requireArguments()
        val level = arguments.getInt(GameFragment.ARG_LEVEL, 1)
        score = arguments.getInt(GameFragment.ARG_SCORE, 0)
        val passed = arguments.getBoolean(GameFragment.ARG_PASSED, false)
        val reward = arguments.getInt(GameFragment.ARG_REWARD_COINS, 0)
        val distance = arguments.getInt(GameFragment.ARG_DISTANCE, 0)
        val feathers = arguments.getInt(GameFragment.ARG_FEATHERS, 0)
        val coins = arguments.getInt(GameFragment.ARG_COINS_COLLECTED, 0)
        val lives = arguments.getInt(GameFragment.ARG_LIVES, 0)
        val streak = arguments.getInt(GameFragment.ARG_BEST_STREAK, 0)
        val skinId = arguments.getString(GameFragment.ARG_SKIN_ID, "hen_classic") ?: "hen_classic"

        binding.winLogoImageView.isVisible = passed
        binding.titleTextView.isVisible = !passed
        // Win and lose use the same panel artwork and exactly the same panel height.
        binding.resultPanel.setBackgroundResource(R.drawable.bg_result_panel)
        binding.titleTextView.text = getString(
            if (passed) R.string.result_win_title else R.string.result_lose_title
        )
        binding.starsRow.isVisible = passed
        binding.rewardsRow.isVisible = passed
        binding.bestTextView.isVisible = false
        binding.titleTextView.setBackgroundResource(android.R.color.transparent)
        binding.titleTextView.setTextColor(requireContext().getColor(R.color.hen_brown))
        binding.subtitleTextView.text = if (passed) {
            getString(R.string.result_win_subtitle, level)
        } else {
            getString(R.string.result_lose_subtitle)
        }
        binding.statsTextView.text = getString(
            R.string.result_stats_format,
            distance,
            feathers,
            coins,
            streak,
            lives
        )
        binding.rewardTextView.text = "+$reward"
        binding.featherRewardTextView.text = "+$feathers"
        binding.rewardTextView.isVisible = reward > 0
        val earnedStars = lives.coerceIn(1, 3)
        binding.starOne.setImageResource(if (earnedStars >= 1) R.drawable.ic_star_active else R.drawable.ic_star_inactive)
        binding.starTwo.setImageResource(if (earnedStars >= 2) R.drawable.ic_star_active else R.drawable.ic_star_inactive)
        binding.starThree.setImageResource(if (earnedStars >= 3) R.drawable.ic_star_active else R.drawable.ic_star_inactive)
        binding.nextButton.isVisible = passed && level < HenLevelConfig.MAX_LEVEL
        binding.restartButton.isVisible = !passed
        binding.menuButton.isVisible = true

        observeBestScore()
        binding.nextButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            openLevel(level + 1)
        }
        binding.restartButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            openLevel(level)
        }
        binding.menuButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            findNavController().popBackStack(R.id.menuFragment, false)
        }
        viewModel.loadBestScore()
    }

    private fun observeBestScore() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bestScore.collect { best ->
                    binding.bestTextView.text = getString(R.string.result_best_format, best)
                }
            }
        }
    }

    private fun openLevel(level: Int) {
        findNavController().navigate(
            R.id.action_resultFragment_to_gameFragment,
            bundleOf(GameFragment.ARG_LEVEL to level.coerceIn(1, HenLevelConfig.MAX_LEVEL))
        )
    }

    override fun onResume() {
        super.onResume()
        soundManager.playMusic(SoundManager.MusicTrack.Game)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
