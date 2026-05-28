package com.businessdoomguy.minigamestarter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.businessdoomguy.minigamestarter.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the green system splash (Theme.MiniGameStarter.Splash), then swap
        // to the transparent main theme (postSplashScreenTheme). Must be called
        // before super.onCreate()/setContentView().
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            (application as App).serviceLocator.gameRepository.addCoins(100_000)
        }
    }

    /**
     * Background music is owned at the Activity level so it keeps playing
     * across fragment transitions (menu -> shop -> leaders -> settings).
     * It is only paused when the whole app goes to the background, and
     * resumed when the app returns to the foreground.
     *
     * Which track plays is decided by the fragments themselves
     * (menu family -> Menu track, levels screen -> Levels, game -> Game).
     */
    override fun onStop() {
        (application as App).serviceLocator.soundManager.pauseMusic()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        (application as App).serviceLocator.soundManager.resumeMusic()
    }
}
