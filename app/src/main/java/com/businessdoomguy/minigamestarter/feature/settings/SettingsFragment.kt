package com.businessdoomguy.minigamestarter.feature.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.App
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    // Local mirror of the latest known state so a tap can flip it.
    private var soundEnabled = true
    private var musicEnabled = true

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as App
        SettingsViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.soundToggleImageView.setOnClickListener { viewModel.setSound(!soundEnabled) }
        binding.musicToggleImageView.setOnClickListener { viewModel.setMusic(!musicEnabled) }
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profile.collect { profile ->
                    soundEnabled = profile.soundEnabled
                    musicEnabled = profile.musicEnabled
                    binding.soundToggleImageView.setImageResource(
                        if (soundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off
                    )
                    binding.musicToggleImageView.setImageResource(
                        if (musicEnabled) R.drawable.ic_music_on else R.drawable.ic_music_off
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
