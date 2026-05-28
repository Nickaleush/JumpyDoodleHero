package com.businessdoomguy.minigamestarter.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.businessdoomguy.minigamestarter.App
import com.businessdoomguy.minigamestarter.R
import com.businessdoomguy.minigamestarter.core.audio.SoundManager
import com.businessdoomguy.minigamestarter.data.db.entity.InventoryItemEntity
import com.businessdoomguy.minigamestarter.data.repository.GameRepository
import com.businessdoomguy.minigamestarter.databinding.FragmentShopBinding
import com.businessdoomguy.minigamestarter.databinding.ItemShopCardBinding
import com.businessdoomguy.minigamestarter.games.jumper.JumperPreviewView
import kotlinx.coroutines.launch

class ShopFragment : Fragment(R.layout.fragment_shop) {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ShopViewModel by viewModels {
        val app = requireActivity().application as App
        ShopViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShopBinding.bind(view)
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
                launch {
                    viewModel.coins.collect { coins ->
                        binding.coinsTextView.text = getString(R.string.shop_coins_format, coins)
                    }
                }
                launch {
                    viewModel.items.collect { renderItems(it) }
                }
                launch {
                    viewModel.messages.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderItems(items: List<InventoryItemEntity>) {
        if (_binding == null) return
        binding.shopContainer.removeAllViews()
        addSectionTitle(getString(R.string.shop_section_skins))
        items.filter { it.type == GameRepository.TYPE_SKIN }.forEach(::addItemCard)
        addSectionTitle(getString(R.string.shop_section_backgrounds))
        items.filter { it.type == GameRepository.TYPE_BACKGROUND }.forEach(::addItemCard)
    }

    private fun addSectionTitle(title: String) {
        val titleView = TextView(requireContext()).apply {
            text = title
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 24f
            typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.bebas)
            setPadding(4, 18, 4, 10)
        }
        binding.shopContainer.addView(titleView)
    }

    /**
     * Inflates one shop card from `item_shop_card.xml` and binds [item] to it.
     *
     * The badge slot shows the supplied badge artwork when available
     * ([ShopArtwork]); for items without dedicated art it falls back to the
     * procedural [JumperPreviewView] so every card still shows a preview.
     */
    private fun addItemCard(item: InventoryItemEntity) {
        val inflater = LayoutInflater.from(requireContext())
        val cardBinding = ItemShopCardBinding.inflate(
            inflater, binding.shopContainer, false
        )

        cardBinding.itemTitleTextView.text = item.title

        // --- Badge artwork or procedural fallback ---------------------------
        val badgeRes = ShopArtwork.badgeFor(item.id)
        if (badgeRes != 0) {
            cardBinding.itemBadgeImageView.setImageResource(badgeRes)
        } else {
            // No badge art: drop a JumperPreviewView into the badge slot.
            val badge = cardBinding.itemBadgeImageView
            val parent = badge.parent as ViewGroup
            val index = parent.indexOfChild(badge)
            val params = badge.layoutParams
            parent.removeViewAt(index)
            val preview = JumperPreviewView(requireContext()).apply {
                id = badge.id
                layoutParams = params
                if (item.type == GameRepository.TYPE_SKIN) showSkin(item.id)
                else showBackground(item.id)
            }
            parent.addView(preview, index)
        }

        // --- Price plate vs action button -----------------------------------
        if (item.isUnlocked) {
            // Owned: show the Use / Selected button, hide the price plate.
            cardBinding.itemPricePlate.visibility = View.GONE
            cardBinding.itemActionButton.visibility = View.VISIBLE
            cardBinding.itemActionButton.isEnabled = !item.isSelected
            cardBinding.itemActionButton.alpha = if (item.isSelected) 1f else 0.5f
            cardBinding.itemActionButton.setOnClickListener {
                viewModel.buyOrSelect(item.id)
            }
        } else {
            // Not owned: show the price plate, tap the whole card to buy.
            cardBinding.itemPricePlate.visibility = View.VISIBLE
            cardBinding.itemActionButton.visibility = View.GONE
            cardBinding.itemPriceTextView.text = item.price.toString()
            cardBinding.root.setOnClickListener { viewModel.buyOrSelect(item.id) }
        }

        binding.shopContainer.addView(cardBinding.root)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
