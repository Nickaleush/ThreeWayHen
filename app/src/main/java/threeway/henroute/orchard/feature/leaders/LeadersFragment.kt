package threeway.henroute.orchard.feature.leaders

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import threeway.henroute.orchard.databinding.FragmentLeadersBinding

class LeadersFragment : SafeAreaFragment(R.layout.fragment_leaders) {
    private var _binding: FragmentLeadersBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val soundManager get() =
        (requireActivity().application as App).serviceLocator.soundManager
    private val viewModel: LeadersViewModel by viewModels {
        LeadersViewModelFactory(
            (requireActivity().application as App).serviceLocator.gameRepository
        )
    }
    private var allTime = false
    private var lastState = LeadersUiState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLeadersBinding.bind(view)
        binding.backButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            findNavController().openMenu()
        }
        binding.weekTab.setOnClickListener { selectTab(false) }
        binding.allTimeTab.setOnClickListener { selectTab(true) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect {
                    lastState = it
                    render(it)
                }
            }
        }
    }

    private fun selectTab(showAllTime: Boolean) {
        allTime = showAllTime
        soundManager.playEffect(SoundManager.SoundEffect.Click)
        binding.weekTab.setBackgroundResource(
            if (!allTime) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.allTimeTab.setBackgroundResource(
            if (allTime) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.weekTab.setTextColor(requireContext().getColor(if (!allTime) R.color.hen_brown else R.color.white))
        binding.allTimeTab.setTextColor(requireContext().getColor(if (allTime) R.color.hen_brown else R.color.white))
        render(lastState)
    }

    private fun render(state: LeadersUiState) {
        binding.statsTextView.text = getString(
            R.string.leaders_stats_format,
            state.stats.levelsPassed,
            state.stats.highestLevelPassed,
            state.stats.totalDistance,
            state.stats.bestStreak
        )
        binding.leadersContainer.removeAllViews()

        val userScore = state.scores.maxOfOrNull { it.score } ?: 21_080
        val rows = if (allTime) ALL_TIME_ROWS else WEEK_ROWS
        rows.forEachIndexed { index, entry ->
            val isUser = entry.first == "You"
            val score = if (isUser) userScore else entry.second
            binding.leadersContainer.addView(createLeaderRow(index + 1, entry.first, score, isUser))
        }
    }

    private fun createLeaderRow(rank: Int, name: String, score: Int, isUser: Boolean): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(10), dp(4))
            setBackgroundResource(
                when {
                    isUser -> R.drawable.bg_leader_row_you
                    rank == 1 -> R.drawable.bg_leader_row_gold
                    else -> R.drawable.bg_leader_row
                }
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { bottomMargin = dp(5) }

            addView(OutlinedTextView(requireContext()).apply {
                text = rank.toString()
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(requireContext().getColor(if (isUser) R.color.white else R.color.hen_brown))
                setShadowLayer(0f, 0f, 0f, 0)
                AppFonts.apply(this)
            }, LinearLayout.LayoutParams(dp(25), LinearLayout.LayoutParams.MATCH_PARENT))

            addView(ImageView(requireContext()).apply {
                setImageResource(avatarFor(name, score, isUser))
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = null
            }, LinearLayout.LayoutParams(dp(38), dp(38)))

            addView(OutlinedTextView(requireContext()).apply {
                text = name
                gravity = Gravity.CENTER_VERTICAL
                textSize = 12f
                setTextColor(requireContext().getColor(if (isUser) R.color.white else R.color.hen_brown))
                setShadowLayer(0f, 0f, 0f, 0)
                AppFonts.apply(this)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(6)
            })

            addView(OutlinedTextView(requireContext()).apply {
                text = String.format("%,d", score)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                textSize = 12f
                setTextColor(requireContext().getColor(if (isUser) R.color.white else R.color.hen_brown))
                setShadowLayer(0f, 0f, 0f, 0)
                AppFonts.apply(this)
            }, LinearLayout.LayoutParams(dp(82), LinearLayout.LayoutParams.MATCH_PARENT))
        }
    }


    private fun avatarFor(name: String, score: Int, isUser: Boolean): Int {
        if (isUser) return R.drawable.avatar_0
        val stableIndex = ((name.hashCode() * 31L + score.toLong()).ushr(1) % AVATARS.size).toInt()
        return AVATARS[stableIndex]
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
        private val WEEK_ROWS = listOf(
            "CluckyMaster" to 152_480,
            "HenSolo" to 128_750,
            "Eggspert" to 105_930,
            "You" to 21_080,
            "PeckQueen" to 88_610,
            "FeatherFury" to 72_340,
            "BarnBaron" to 63_210,
            "CackleKing" to 49_320,
            "CornClucker" to 23_450,
            "ChickNorris" to 20_960,
            "SirClucksALot" to 20_340
        )
        private val ALL_TIME_ROWS = listOf(
            "CluckyMaster" to 942_400,
            "HenSolo" to 881_750,
            "Eggspert" to 760_930,
            "PeckQueen" to 639_610,
            "FeatherFury" to 581_340,
            "BarnBaron" to 522_210,
            "You" to 21_080,
            "CackleKing" to 419_320,
            "CornClucker" to 353_450,
            "ChickNorris" to 320_960,
            "SirClucksALot" to 298_340
        )
        private val AVATARS = intArrayOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7
        )    }
}
