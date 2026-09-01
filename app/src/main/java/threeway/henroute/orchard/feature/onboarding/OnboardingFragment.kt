package threeway.henroute.orchard.feature.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import threeway.henroute.orchard.databinding.FragmentOnboardingBinding

class OnboardingFragment : SafeAreaFragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var page = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)
        page = (savedInstanceState?.getInt(KEY_PAGE) ?: 0).coerceIn(0, LAST_PAGE)
        binding.nextButton.setOnClickListener { next() }
        binding.backButton.setOnClickListener { back() }
        render()
    }

    private fun next() {
        if (page < LAST_PAGE) {
            page++
            render()
        } else {
            requireContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DONE, true)
                .apply()
            findNavController().navigate(R.id.action_onboardingFragment_to_menuFragment)
        }
    }

    private fun back() {
        if (page > 0) {
            page--
            render()
        }
    }

    private fun render() {
        val imageRes = when (page) {
            0 -> R.drawable.onboarding_screen_1
            1 -> R.drawable.onboarding_screen_2
            else -> R.drawable.onboarding_screen_3
        }
        binding.root.setBackgroundResource(imageRes)
        binding.illustrationImageView.setImageResource(imageRes)
        binding.bodyTextView.setText(
            when (page) {
                0 -> R.string.onboarding_text_1
                1 -> R.string.onboarding_text_2
                else -> R.string.onboarding_text_3
            }
        )
        binding.nextButton.setText(
            if (page == LAST_PAGE) R.string.onboarding_start else R.string.onboarding_next
        )
        binding.dotOne.setBackgroundResource(if (page == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        binding.dotTwo.setBackgroundResource(if (page == 1) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        binding.dotThree.setBackgroundResource(if (page == 2) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, page)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val PREFS = "three_way_hen_prefs"
        const val KEY_DONE = "onboarding_done"
        private const val KEY_PAGE = "page"
        private const val LAST_PAGE = 2
    }
}
