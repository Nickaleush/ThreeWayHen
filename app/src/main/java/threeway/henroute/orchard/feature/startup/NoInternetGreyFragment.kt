package threeway.henroute.orchard.feature.startup

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import threeway.henroute.orchard.databinding.NoInternetGreyFragmentBinding

class NoInternetGreyFragment : SafeAreaFragment(R.layout.no_internet_grey_fragment) {

    override val applySafeArea: Boolean
        get() = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    private var _binding: NoInternetGreyFragmentBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = NoInternetGreyFragmentBinding.bind(view)
        binding.retryButton.setOnClickListener { retry() }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun retry() {
        // This screen is a hard startup gate: never continue until Android reports
        // a validated internet connection. A local Wi-Fi connection without actual
        // internet access is not enough.
        if (!StartupGate.isInternetAvailable(requireContext())) return

        val action = when {
            StartupGate.shouldShowNotificationPrompt(requireContext()) ->
                R.id.action_noInternetGreyFragment_to_onBoardingGreyFragment

            StartupGate.isGameOnboardingDone(requireContext()) ->
                R.id.action_noInternetGreyFragment_to_menuFragment

            else -> R.id.action_noInternetGreyFragment_to_onboardingFragment
        }
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
