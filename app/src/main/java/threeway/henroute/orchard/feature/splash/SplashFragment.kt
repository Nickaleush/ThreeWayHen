package threeway.henroute.orchard.feature.splash

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.databinding.SplashFragmentBinding
import threeway.henroute.orchard.feature.startup.StartupGate

class SplashFragment : SafeAreaFragment(R.layout.splash_fragment) {

    private var _binding: SplashFragmentBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var fillAnimator: ObjectAnimator? = null
    private var didNavigate = false
    private var startUptimeMs = 0L
    private val navigateRunnable = Runnable { navigateNext() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SplashFragmentBinding.bind(view)
        startUptimeMs = savedInstanceState?.getLong(KEY_START_UPTIME) ?: SystemClock.uptimeMillis()
        binding.progressBarFillView.scaleX = 0f
    }

    override fun onResume() {
        super.onResume()
        val services = (requireActivity().application as App).serviceLocator
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = services.gameRepository.getProfile()
            services.soundManager.applySettings(
                soundEnabled = profile.soundEnabled,
                musicEnabled = profile.musicEnabled,
                soundVolume = profile.soundVolume / 100f,
                musicVolume = profile.musicVolume / 100f
            )
            services.soundManager.playMusic(SoundManager.MusicTrack.Menu)
        }
        scheduleNavigationAndAnimate()
    }

    override fun onPause() {
        super.onPause()
        _binding?.root?.removeCallbacks(navigateRunnable)
        fillAnimator?.cancel()
        fillAnimator = null
    }

    private fun scheduleNavigationAndAnimate() {
        if (didNavigate) return
        val binding = _binding ?: return
        val elapsed = SystemClock.uptimeMillis() - startUptimeMs
        val remaining = SPLASH_DURATION_MS - elapsed
        if (remaining <= 0L) {
            navigateNext()
            return
        }
        val startFraction = (elapsed.toFloat() / SPLASH_DURATION_MS).coerceIn(0f, 1f)
        binding.progressBarFillView.scaleX = startFraction
        fillAnimator = ObjectAnimator.ofFloat(binding.progressBarFillView, View.SCALE_X, startFraction, 1f).apply {
            duration = remaining
            interpolator = DecelerateInterpolator()
            start()
        }
        binding.root.removeCallbacks(navigateRunnable)
        binding.root.postDelayed(navigateRunnable, remaining)
    }

    private fun navigateNext() {
        if (didNavigate || !isAdded || !isResumed) return
        didNavigate = true
        _binding?.root?.removeCallbacks(navigateRunnable)
        val action = when {
            !StartupGate.isInternetAvailable(requireContext()) -> R.id.action_splashFragment_to_noInternetGreyFragment
            StartupGate.shouldShowNotificationPrompt(requireContext()) -> R.id.action_splashFragment_to_onBoardingGreyFragment
            StartupGate.isGameOnboardingDone(requireContext()) -> R.id.action_splashFragment_to_menuFragment
            else -> R.id.action_splashFragment_to_onboardingFragment
        }
        findNavController().navigate(action)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_START_UPTIME, startUptimeMs)
    }

    override fun onDestroyView() {
        _binding?.root?.removeCallbacks(navigateRunnable)
        fillAnimator?.cancel()
        fillAnimator = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1600L
        const val KEY_START_UPTIME = "splash_start_uptime"
    }
}
