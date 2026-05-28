package com.businessdoomguy.minigamestarter.feature.splash

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.databinding.FragmentSplashBinding

/**
 * Splash screen.
 *
 * The full artwork (logo, hero, space background) is the supplied image set
 * as the screen background. The only animated element is the loading
 * progress bar pinned to the bottom of the screen; when it finishes filling
 * the app navigates to the main menu.
 *
 * Backgrounding behaviour:
 * Navigation is driven by elapsed wall-clock time rather than the progress
 * animator's end callback. If the user backgrounds the app while the splash
 * is showing, the animator may be paused/cancelled by the system and its
 * `doOnEnd` callback may never fire (or fire while the fragment is stopped,
 * where navigating would throw). On return we re-check how much time has
 * actually elapsed and either navigate immediately (duration already passed)
 * or resume the bar for the remaining time. Navigation is only ever performed
 * while the fragment is resumed, so we never lose it to a backgrounded state.
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var fillAnimator: ObjectAnimator? = null
    private var didNavigate = false

    /** Wall-clock time (uptime millis) at which the splash first started. */
    private var startUptimeMs = 0L

    /** Posted "time is up, navigate now" task, so we can cancel it cleanly. */
    private val navigateRunnable = Runnable { navigateToMenu() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        // Restore (or set) the absolute start time so config changes / recreation
        // don't restart the countdown from zero.
        startUptimeMs = savedInstanceState?.getLong(KEY_START_UPTIME)
            ?: SystemClock.uptimeMillis()

        binding.progressBarFillView.scaleX = 0f
    }

    override fun onResume() {
        super.onResume()
        scheduleNavigationAndAnimate()
    }

    override fun onPause() {
        super.onPause()
        // Stop the visual fill and the pending navigation while we're not in the
        // foreground. Both are rebuilt from the elapsed time in onResume().
        _binding?.root?.removeCallbacks(navigateRunnable)
        fillAnimator?.cancel()
        fillAnimator = null
    }

    private fun scheduleNavigationAndAnimate() {
        if (didNavigate) return
        val binding = _binding ?: return

        val elapsed = SystemClock.uptimeMillis() - startUptimeMs
        val remaining = SPLASH_DURATION_MS - elapsed

        if (remaining <= 0L) {
            // The full splash duration already passed (e.g. the user spent a
            // while in the background). Go straight to the menu.
            navigateToMenu()
            return
        }

        // Resume the progress bar from where it should be for the remaining time.
        val startFraction = (elapsed.toFloat() / SPLASH_DURATION_MS).coerceIn(0f, 1f)
        binding.progressBarFillView.scaleX = startFraction
        fillAnimator = ObjectAnimator.ofFloat(
            binding.progressBarFillView, View.SCALE_X, startFraction, 1f
        ).apply {
            duration = remaining
            interpolator = DecelerateInterpolator()
            start()
        }

        // Navigation is driven by the timer (not the animator callback) so it is
        // robust even if the animator is interrupted.
        binding.root.removeCallbacks(navigateRunnable)
        binding.root.postDelayed(navigateRunnable, remaining)
    }

    private fun navigateToMenu() {
        if (didNavigate || !isAdded) return
        // Only navigate when the fragment is actually resumed; otherwise defer to
        // the next onResume(), which reschedules based on elapsed time.
        if (!isResumed) return
        didNavigate = true
        _binding?.root?.removeCallbacks(navigateRunnable)
        findNavController().navigate(R.id.action_splashFragment_to_menuFragment)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_START_UPTIME, startUptimeMs)
    }

    override fun onDestroyView() {
        _binding?.root?.removeCallbacks(navigateRunnable)
        fillAnimator?.cancel()
        fillAnimator = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1600L
        const val KEY_START_UPTIME = "splash_start_uptime"
    }
}
