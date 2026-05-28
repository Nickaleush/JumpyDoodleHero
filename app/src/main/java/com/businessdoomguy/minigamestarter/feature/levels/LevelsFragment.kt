package com.businessdoomguy.minigamestarter.feature.levels

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.App
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.databinding.FragmentLevelsBinding
import com.businessdoomguy.minigamestarter.feature.game.GameFragment
import com.businessdoomguy.minigamestarter.games.jumper.JumperLevelConfig
import kotlinx.coroutines.launch

class LevelsFragment : Fragment(R.layout.fragment_levels) {

    private var _binding: FragmentLevelsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: LevelsViewModel by viewModels {
        val app = requireActivity().application as App
        LevelsViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLevelsBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        observeProgress()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Levels)
    }

    override fun onPause() {
        (requireActivity().application as App).serviceLocator.soundManager.pauseMusic()
        super.onPause()
    }

    private fun observeProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.maxUnlockedLevel.collect { maxUnlocked ->
                    renderLevels(maxUnlocked)
                }
            }
        }
    }

    private fun renderLevels(maxUnlockedLevel: Int) {
        if (_binding == null) return
        binding.levelsGridLayout.removeAllViews()

        val margin = resources.getDimensionPixelSize(R.dimen.level_button_margin)
        val size = resources.getDimensionPixelSize(R.dimen.level_button_size)

        for (level in 1..LEVEL_COUNT) {
            val config = JumperLevelConfig.forLevel(level)
            val unlocked = level <= maxUnlockedLevel

            val button = Button(requireContext()).apply {
                text = if (unlocked) {
                    level.toString()
                } else {
                    getString(R.string.level_locked_symbol)
                }

                textSize = 20f
                isAllCaps = false

                setTextColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.white
                    )
                )

                // Шрифт Bebas
                typeface = ResourcesCompat.getFont(
                    context,
                    R.font.bebas
                )

                alpha = if (unlocked) 1f else 0.45f

                background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_small_button,
                    null
                )

                contentDescription = if (unlocked) {
                    getString(
                        R.string.level_button_description,
                        level,
                        config.targetHeightMeters
                    )
                } else {
                    getString(
                        R.string.level_locked_description,
                        level,
                        level - 1
                    )
                }

                setOnClickListener {
                    if (unlocked) {
                        openLevel(level)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.level_locked_toast, level - 1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(margin, margin, margin, margin)
            }
            binding.levelsGridLayout.addView(button, params)
        }
    }

    private fun openLevel(level: Int) {
        findNavController().navigate(
            R.id.action_levelsFragment_to_gameFragment,
            bundleOf(GameFragment.ARG_LEVEL to level)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val LEVEL_COUNT = 45
    }
}
