package threeway.henroute.orchard.feature.levels

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.core.navigation.openMenu
import threeway.henroute.orchard.core.ui.AppFonts
import threeway.henroute.orchard.core.ui.OutlinedTextView
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import threeway.henroute.orchard.databinding.FragmentLevelsBinding
import threeway.henroute.orchard.feature.game.GameFragment
import threeway.henroute.orchard.games.hen.HenLevelConfig

class LevelsFragment : SafeAreaFragment(R.layout.fragment_levels) {
    private var _binding: FragmentLevelsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val soundManager get() =
        (requireActivity().application as App).serviceLocator.soundManager
    private val viewModel: LevelsViewModel by viewModels {
        LevelsViewModelFactory(
            (requireActivity().application as App).serviceLocator.gameRepository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLevelsBinding.bind(view)
        binding.backButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            findNavController().openMenu()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.coinTextView.text = getString(R.string.coins_format, state.coins)
                    binding.levelsGridLayout.post { renderLevels(state) }
                }
            }
        }
    }

    private fun renderLevels(state: LevelsUiState) {
        val grid = binding.levelsGridLayout
        val width = grid.width
        if (width <= 0) return
        grid.removeAllViews()
        val margin = dp(5)
        val size = ((width - margin * 2 * COLUMNS) / COLUMNS).coerceAtLeast(dp(56))

        for (level in 1..HenLevelConfig.MAX_LEVEL) {
            val unlocked = level <= state.maxUnlocked
            val completed = level <= state.highestPassed
            val card = createLevelCard(level, unlocked, completed)
            val params = GridLayout.LayoutParams().apply {
                this.width = size
                height = size
                setMargins(margin, margin, margin, margin)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(card, params)
        }
    }

    private fun createLevelCard(level: Int, unlocked: Boolean, completed: Boolean): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(4), dp(5), dp(4), dp(4))
            setBackgroundResource(
                when {
                    !unlocked -> R.drawable.bg_level_locked
                    completed -> R.drawable.bg_level_completed
                    else -> R.drawable.bg_level_active
                }
            )
            contentDescription = if (unlocked) {
                getString(
                    R.string.level_button_description,
                    level,
                    HenLevelConfig.displayDifficulty(level)
                )
            } else {
                getString(R.string.level_locked_description, level, level - 1)
            }
            setOnClickListener {
                soundManager.playEffect(SoundManager.SoundEffect.Click)
                if (unlocked) {
                    findNavController().navigate(
                        R.id.action_levelsFragment_to_gameFragment,
                        bundleOf(GameFragment.ARG_LEVEL to level)
                    )
                } else {
                    soundManager.playEffect(SoundManager.SoundEffect.Error)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.level_locked_toast, level - 1),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (!unlocked) {
            card.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_lock)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            })
            return card
        }

        card.addView(OutlinedTextView(requireContext()).apply {
            text = level.toString()
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(requireContext().getColor(R.color.hen_brown))
            setShadowLayer(0f, 0f, 0f, 0)
            AppFonts.apply(this)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        })

        val stars = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(3) {
            stars.addView(ImageView(requireContext()).apply {
                setImageResource(if (completed) R.drawable.ic_star_active else R.drawable.ic_star_inactive)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(15), dp(15)).apply {
                    if (it > 0) marginStart = dp(1)
                }
            })
        }
        card.addView(stars, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(18)
        ))
        return card
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        viewModel.load()
        soundManager.playMusic(SoundManager.MusicTrack.Menu)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val COLUMNS = 4
    }
}
