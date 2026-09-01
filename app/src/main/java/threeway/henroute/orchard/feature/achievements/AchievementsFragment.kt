package threeway.henroute.orchard.feature.achievements

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import threeway.henroute.orchard.data.db.entity.AchievementEntity
import threeway.henroute.orchard.databinding.FragmentAchievementsBinding

class AchievementsFragment : SafeAreaFragment(R.layout.fragment_achievements) {
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val soundManager get() =
        (requireActivity().application as App).serviceLocator.soundManager
    private val viewModel: AchievementsViewModel by viewModels {
        AchievementsViewModelFactory(
            (requireActivity().application as App).serviceLocator.gameRepository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAchievementsBinding.bind(view)
        binding.backButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            findNavController().openMenu()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    binding.achievementsContainer.post { render(items) }
                }
            }
        }
    }

    private fun render(items: List<AchievementEntity>) {
        val grid = binding.achievementsContainer
        val width = grid.width
        if (width <= 0) return
        grid.removeAllViews()
        val margin = dp(5)
        val cardWidth = ((width - margin * 4) / 2).coerceAtLeast(dp(130))

        items.forEach { item ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setBackgroundResource(if (item.isUnlocked) R.drawable.bg_achievement_card else R.drawable.achievement_inactive_bg)
            }

            card.addView(ImageView(requireContext()).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(achievementArtwork(item.id, item.isUnlocked))
                contentDescription = item.title
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ))

            card.addView(OutlinedTextView(requireContext()).apply {
                text = item.title
                gravity = Gravity.CENTER
                maxLines = 1
                textSize = 10.5f
                setTextColor(requireContext().getColor(R.color.hen_brown))
                setShadowLayer(0f, 0f, 0f, 0)
                AppFonts.apply(this)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25)
            ))

            card.addView(ProgressBar(
                requireContext(),
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = item.targetValue.coerceAtLeast(1)
                progress = item.currentValue.coerceAtMost(max)
                progressDrawable = requireContext().getDrawable(R.drawable.progress_achievement)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply {
                marginStart = dp(10)
                marginEnd = dp(10)
                bottomMargin = dp(2)
            })

            card.addView(OutlinedTextView(requireContext()).apply {
                text = getString(
                    R.string.achievement_progress_format,
                    item.currentValue,
                    item.targetValue
                )
                gravity = Gravity.CENTER
                textSize = 8f
                setTextColor(requireContext().getColor(R.color.hen_brown))
                setShadowLayer(0f, 0f, 0f, 0)
                AppFonts.apply(this)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(16)
            ))

            grid.addView(card, GridLayout.LayoutParams().apply {
                this.width = cardWidth
                height = dp(190)
                setMargins(margin, margin, margin, margin)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
    }

    private fun achievementArtwork(id: String, unlocked: Boolean): Int = when (id) {
        "first_branch" -> if (unlocked) R.drawable.achievment_first_branch else R.drawable.achievment_first_branch_inactive
        "feather_path" -> if (unlocked) R.drawable.achievment_feather else R.drawable.achievment_feather_inactive
        "coin_path" -> if (unlocked) R.drawable.achievment_coin else R.drawable.achievment_coin_inactive
        "safe_choice" -> if (unlocked) R.drawable.achievment_safe_choice else R.drawable.achievment_safe_choice_inactive
        "three_boss" -> if (unlocked) R.drawable.achievment_boss else R.drawable.achievment_boss_inactive
        "route_king" -> if (unlocked) R.drawable.achievment_route_king else R.drawable.achievment_route_king_inactive
        "preview_fan" -> if (unlocked) R.drawable.achievment_skin_king else R.drawable.achievment_skin_king_inactive
        "marathon_hen" -> if (unlocked) R.drawable.achievment_marathon_king else R.drawable.achievment_marathon_king_inactive
        "no_hit" -> if (unlocked) R.drawable.achievment_no_hit else R.drawable.achievment_no_hit_inactive
        else -> if (unlocked) R.drawable.achievment_all_branches else R.drawable.achievment_all_branches_inactive
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
}
