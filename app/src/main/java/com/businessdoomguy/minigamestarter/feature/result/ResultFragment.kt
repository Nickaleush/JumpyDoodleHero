package com.businessdoomguy.minigamestarter.feature.result

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.App
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.databinding.FragmentResultBinding
import com.businessdoomguy.minigamestarter.feature.game.GameFragment
import kotlinx.coroutines.launch

class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ResultViewModel by viewModels {
        val app = requireActivity().application as App
        ResultViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val level = requireArguments().getInt(GameFragment.ARG_LEVEL, 1)
        val score = requireArguments().getInt(GameFragment.ARG_SCORE, 0)
        val heightMeters = requireArguments().getInt(GameFragment.ARG_HEIGHT, 0)
        val passed = requireArguments().getBoolean(GameFragment.ARG_PASSED, false)
        val rewardCoins = requireArguments().getInt(GameFragment.ARG_REWARD_COINS, 0)

        binding.resultTitleTextView.text = if (passed) "Level passed" else "Game Over"
        binding.resultScoreTextView.text = getString(R.string.result_score_format, score)
        binding.resultHeightTextView.text = getString(R.string.result_height_format, heightMeters)
        binding.rewardTextView.text = getString(R.string.result_reward_format, rewardCoins)
        binding.nextButton.isVisible = passed && level < 45
        binding.restartButton.isVisible = !passed || level == 45
        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_resultFragment_to_gameFragment,
                bundleOf(GameFragment.ARG_LEVEL to (level + 1).coerceAtMost(45))
            )
        }

        binding.restartButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_resultFragment_to_gameFragment,
                bundleOf(GameFragment.ARG_LEVEL to level)
            )
        }

        binding.menuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
        }

        viewModel.loadBestScore()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
