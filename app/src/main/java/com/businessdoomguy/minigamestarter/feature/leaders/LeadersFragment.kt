package com.businessdoomguy.minigamestarter.feature.leaders

import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.App
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.data.db.entity.AchievementEntity
import com.businessdoomguy.minigamestarter.databinding.FragmentLeadersBinding
import kotlinx.coroutines.launch

class LeadersFragment : Fragment(R.layout.fragment_leaders) {

    private var _binding: FragmentLeadersBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: LeadersViewModel by viewModels {
        val app = requireActivity().application as App
        LeadersViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLeadersBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        // Keep the menu background music playing on this screen.
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.achievements.collect { render() } }
            }
        }
    }

    private fun render() {
        binding.leadersContainer.removeAllViews()
        addTitle("Achievements")
        val achievements = viewModel.achievements.value
        if (achievements.isEmpty()) {
            addText("No achievements yet. Keep playing to unlock them.")
        } else {
            achievements.forEach(::addAchievement)
        }
    }

    private fun addTitle(text: String) {
        binding.leadersContainer.addView(TextView(requireContext()).apply {
            this.text = text
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 24f
            typeface = ResourcesCompat.getFont(requireContext(), R.font.bebas)
            setPadding(dp(4), dp(18), dp(4), dp(10))
        })
    }

    private fun addText(text: String) {
        binding.leadersContainer.addView(TextView(requireContext()).apply {
            this.text = text
            setTextColor(resources.getColor(R.color.jumper_text_primary, null))
            textSize = 16f
            setBackgroundResource(R.drawable.bg_overlay_panel)
            setPadding(dp(18))
        })
    }

    private fun addAchievement(achievement: AchievementEntity) {
        val status = if (achievement.isUnlocked) {
            getString(R.string.leaders_achievement_done)
        } else {
            getString(
                R.string.leaders_achievement_progress_format,
                achievement.currentValue,
                achievement.targetValue
            )
        }

        // ---- Card: fixed 200dp height, item_bg background ----
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.item_bg)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(200)
            )
            params.setMargins(0, 0, 0, dp(14))
            layoutParams = params
        }

        // ---- Trophy icon (large, on the left) ----
        val trophy = ImageView(requireContext()).apply {
            setImageResource(R.drawable.icon_trophy)
            val iconSize = dp(110)
            val params = LinearLayout.LayoutParams(iconSize, iconSize)
            params.marginEnd = dp(16)
            layoutParams = params
            alpha = if (achievement.isUnlocked) 1f else 0.35f
        }

        // ---- Text column (title + description), centered ----
        val textColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        // Title: large gold text, auto-shrinks to fit on one line, words never split.
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = achievement.title
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.jumper_button_primary, null))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.bebas)
            maxLines = 1
            isSingleLine = true
            if (Build.VERSION.SDK_INT >= 23) {
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
            // Autosize: shrinks from 26sp down to 12sp until it fits the width.
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )
        })
        // Description below the title, auto-shrinks, wraps by words (max 2 lines).
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = achievement.description
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.white, null))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.bebas)
            setPadding(0, dp(4), 0, 0)
            maxLines = 2
            if (Build.VERSION.SDK_INT >= 23) {
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 11, 18, 1, TypedValue.COMPLEX_UNIT_SP
            )
        })
        // Optional progress / DONE line (small, dimmer), centered.
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = status
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.jumper_text_secondary, null))
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        })

        // ---- Green check mark for unlocked achievements (right side) ----
        // ic_selected on secondary_items_bg, 80x80dp, like the shop "selected" badge.
        val check = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_selected)
            setBackgroundResource(R.drawable.secondary_items_bg)
            val checkSize = dp(80)
            val params = LinearLayout.LayoutParams(checkSize, checkSize)
            params.marginStart = dp(8)
            layoutParams = params
            setPadding(dp(18), dp(18), dp(18), dp(18))
            visibility = if (achievement.isUnlocked) View.VISIBLE else View.INVISIBLE
        }

        card.addView(trophy)
        card.addView(textColumn)
        card.addView(check)
        binding.leadersContainer.addView(card)
    }

    /** dp -> px helper. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}