package threeway.henroute.orchard.feature.menu

import android.os.Bundle
import android.view.View
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
import threeway.henroute.orchard.databinding.FragmentMenuBinding

class MenuFragment : SafeAreaFragment(R.layout.fragment_menu) {
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val soundManager get() = (requireActivity().application as App).serviceLocator.soundManager

    private val viewModel: MenuViewModel by viewModels {
        val app = requireActivity().application as App
        MenuViewModelFactory(app.serviceLocator.gameRepository, app.serviceLocator.soundManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMenuBinding.bind(view)
        binding.playButton.setOnClickListener { open(R.id.action_menuFragment_to_levelsFragment) }
        binding.shopButton.setOnClickListener { open(R.id.action_menuFragment_to_shopFragment) }
        binding.achievementsButton.setOnClickListener { open(R.id.action_menuFragment_to_achievementsFragment) }
        binding.leadersButton.setOnClickListener { open(R.id.action_menuFragment_to_leadersFragment) }
        binding.settingsButton.setOnClickListener { open(R.id.action_menuFragment_to_settingsFragment) }
        observe()
    }

    private fun open(action: Int) {
        soundManager.playEffect(SoundManager.SoundEffect.Click)
        findNavController().navigate(action)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.coins.collect { binding.coinTextView.text = getString(R.string.coins_format, it) } }
            }
        }
    }

    private fun previewFor(id: String): Int = when (id) {
        "hen_preview" -> R.drawable.skin_blue
        "hen_quick" -> R.drawable.skin_green
        "hen_magnet" -> R.drawable.skin_red
        "hen_auto" -> R.drawable.skin_yellow
        else -> R.drawable.skin_default
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMenuData()
        soundManager.playMusic(SoundManager.MusicTrack.Menu)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
