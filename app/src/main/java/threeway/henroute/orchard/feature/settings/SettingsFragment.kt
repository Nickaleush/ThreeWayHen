package threeway.henroute.orchard.feature.settings

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
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
import threeway.henroute.orchard.core.navigation.openMenu
import threeway.henroute.orchard.databinding.FragmentSettingsBinding

class SettingsFragment : SafeAreaFragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val soundManager: SoundManager
        get() = (requireActivity().application as App).serviceLocator.soundManager

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as App
        SettingsViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    private var rendering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        binding.backButton.setOnClickListener {
            soundManager.playEffect(SoundManager.SoundEffect.Click)
            findNavController().openMenu()
        }
        setupControls()
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        soundManager.playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun setupControls() {
        binding.soundSwitch.setOnCheckedChangeListener { _, enabled ->
            if (!rendering) viewModel.setSound(enabled)
        }
        binding.musicSwitch.setOnCheckedChangeListener { _, enabled ->
            if (!rendering) viewModel.setMusic(enabled)
        }
        binding.soundSeekBar.setOnSeekBarChangeListener(seekBarListener(viewModel::setSoundVolume))
        binding.musicSeekBar.setOnSeekBarChangeListener(seekBarListener(viewModel::setMusicVolume))
    }

    private fun seekBarListener(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser && !rendering) onChanged(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { profile ->
                    rendering = true
                    binding.soundSwitch.isChecked = profile.soundEnabled
                    binding.musicSwitch.isChecked = profile.musicEnabled
                    binding.soundSeekBar.progress = profile.soundVolume
                    binding.musicSeekBar.progress = profile.musicVolume
                    rendering = false
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
