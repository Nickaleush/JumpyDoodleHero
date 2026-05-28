package com.businessdoomguy.minigamestarter.feature.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.databinding.FragmentGameBinding
import com.businessdoomguy.minigamestarter.games.jumper.JumperGameResult
import com.businessdoomguy.minigamestarter.games.jumper.JumperGameView
import kotlinx.coroutines.launch

class GameFragment : Fragment(R.layout.fragment_game), JumperGameView.Callback, SensorEventListener {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: GameViewModel by viewModels {
        val app = requireActivity().application as App
        GameViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    private val soundManager: SoundManager
        get() = (requireActivity().application as App).serviceLocator.soundManager

    private var level = 1
    private var hasFinished = false
    private var isPausedByUser = false
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    // Guards against starting the level more than once. We deliberately wait for
    // the player profile (skin + background) to load and be applied BEFORE the
    // first frame is drawn, so the game never flashes the default look first.
    private var levelStarted = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGameBinding.bind(view)
        level = requireArguments().getInt(ARG_LEVEL, 1).coerceIn(1, 45)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.gameView.callback = this
        setupListeners()
        hidePausePanel()
        observeProfile()
        viewModel.loadPlayerProfile()
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Keep the live appearance in sync on every profile update
                // (e.g. the user changes skin/background and comes back).
                launch {
                    viewModel.profile.collect { profile ->
                        if (_binding == null) return@collect
                        binding.gameView.setAppearance(
                            skinId = profile.selectedSkinId,
                            backgroundId = profile.selectedBackgroundId
                        )
                    }
                }
                // Start the level exactly once, when the real profile has been
                // loaded. profileLoaded is a dedicated false -> true signal, so
                // it always arrives even if the loaded profile equals the
                // default one (which would suppress a second profile emission).
                // By the time this fires, setAppearance above has already run,
                // so there is no default-look flash.
                launch {
                    viewModel.profileLoaded.collect { loaded ->
                        if (loaded && !levelStarted && _binding != null) {
                            levelStarted = true
                            binding.gameView.post {
                                if (_binding != null) {
                                    binding.gameView.startLevel(level)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.pauseButton.setOnClickListener {
            showPausePanel()
        }

        binding.continueButton.setOnClickListener {
            hidePausePanel()
        }

        binding.restartButton.setOnClickListener {
            hidePausePanel()
            hasFinished = false
            binding.gameView.restart()
        }

        binding.homeButton.setOnClickListener {
            binding.gameView.stopGameLoop()
            findNavController().popBackStack(R.id.menuFragment, false)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        // Switch background music to the game track while playing.
        soundManager.playMusic(SoundManager.MusicTrack.Game)
        if (_binding != null && !isPausedByUser && !hasFinished) {
            binding.gameView.setPaused(false)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        soundManager.pauseMusic()
        if (_binding != null && !hasFinished) {
            binding.gameView.setPaused(true)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        binding.gameView.callback = null
        binding.gameView.stopGameLoop()
        _binding = null
        super.onDestroyView()
    }

    override fun onHudChanged(level: Int, heightMeters: Int, targetMeters: Int, score: Int, shieldLives: Int) {
        if (_binding == null) return
        binding.levelTextView.text = getString(R.string.game_level_format, level)
        binding.heightTextView.text = getString(R.string.game_height_format, heightMeters)
        binding.goalTextView.text = getString(R.string.game_goal_format, targetMeters)
    }

    override fun onJumpSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Jump)
    }

    override fun onBoosterSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Booster)
    }

    override fun onMonsterStompSound() {
        soundManager.playEffect(SoundManager.SoundEffect.MonsterStomp)
    }

    override fun onHazardSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Hazard)
    }

    override fun onGameFinished(result: JumperGameResult) {
        if (hasFinished || _binding == null) return
        hasFinished = true

        // End-of-level feedback.
        soundManager.playEffect(
            if (result.passed) SoundManager.SoundEffect.Win else SoundManager.SoundEffect.Lose
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveResult(result)

            if (_binding == null) return@launch

            findNavController().navigate(
                R.id.action_gameFragment_to_resultFragment,
                bundleOf(
                    ARG_LEVEL to result.level,
                    ARG_SCORE to result.score,
                    ARG_PASSED to result.passed,
                    ARG_REWARD_COINS to result.rewardCoins,
                    ARG_HEIGHT to result.heightMeters
                )
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && _binding != null) {
            binding.gameView.setTilt(event.values[0])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun showPausePanel() {
        isPausedByUser = true
        binding.gameView.setPaused(true)
        soundManager.pauseMusic()
        binding.pausePanel.isVisible = true
    }

    private fun hidePausePanel() {
        isPausedByUser = false
        binding.pausePanel.isVisible = false
        if (!hasFinished) {
            binding.gameView.setPaused(false)
            soundManager.playMusic(SoundManager.MusicTrack.Game)
        }
    }

    companion object {
        const val ARG_LEVEL = "level"
        const val ARG_SCORE = "score"
        const val ARG_PASSED = "passed"
        const val ARG_REWARD_COINS = "rewardCoins"
        const val ARG_HEIGHT = "heightMeters"
    }
}