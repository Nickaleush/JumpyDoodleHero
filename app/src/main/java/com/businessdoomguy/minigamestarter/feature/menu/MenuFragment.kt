package com.businessdoomguy.minigamestarter.feature.menu

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
import com.businessdoomguy.minigamestarter.databinding.FragmentMenuBinding
import kotlinx.coroutines.launch

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: MenuViewModel by viewModels {
        val app = requireActivity().application as App
        MenuViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMenuBinding.bind(view)

        setupClickListeners()
        observeMenuData()
    }

    private fun setupClickListeners() {
        binding.playButton.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_levelsFragment)
        }

        binding.shopButton.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_shopFragment)
        }

        binding.leadersButton.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_leadersFragment)
        }

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_menuFragment_to_settingsFragment)
        }
    }

    private fun observeMenuData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.coins.collect { coins ->
                        binding.coinTextView.text = "$coins"
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMenuData()
        val soundManager = (requireActivity().application as App).serviceLocator.soundManager
        soundManager.playMusic(SoundManager.MusicTrack.Menu)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
