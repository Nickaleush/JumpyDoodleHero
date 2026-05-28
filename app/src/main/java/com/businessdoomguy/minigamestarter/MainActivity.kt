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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            (application as App).serviceLocator.gameRepository.addCoins(100_000)
        }
    }

    override fun onStop() {
        (application as App).serviceLocator.soundManager.pauseMusic()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        (application as App).serviceLocator.soundManager.resumeMusic()
    }
}
