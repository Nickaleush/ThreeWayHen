package threeway.henroute.orchard.feature.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import threeway.henroute.orchard.core.ui.SafeAreaFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import threeway.henroute.orchard.App
import threeway.henroute.orchard.R
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.core.navigation.openMenu
import threeway.henroute.orchard.data.db.entity.InventoryItemEntity
import threeway.henroute.orchard.data.repository.GameRepository
import threeway.henroute.orchard.databinding.FragmentShopBinding
import threeway.henroute.orchard.databinding.ItemShopCardBinding

class ShopFragment : SafeAreaFragment(R.layout.fragment_shop) {
    private var _binding: FragmentShopBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val soundManager get() = (requireActivity().application as App).serviceLocator.soundManager
    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory((requireActivity().application as App).serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShopBinding.bind(view)
        binding.backButton.setOnClickListener { soundManager.playEffect(SoundManager.SoundEffect.Click); findNavController().openMenu() }
        observe()
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { state -> binding.coinTextView.text = getString(R.string.coins_format, state.coins); render(state.items) } }
                launch { viewModel.events.collect { event ->
                    when (event) {
                        GameRepository.PurchaseResult.Success -> soundManager.playEffect(SoundManager.SoundEffect.Click)
                        GameRepository.PurchaseResult.NotEnoughCoins -> { soundManager.playEffect(SoundManager.SoundEffect.Error); Toast.makeText(requireContext(), R.string.shop_not_enough, Toast.LENGTH_SHORT).show() }
                        GameRepository.PurchaseResult.NotFound -> { soundManager.playEffect(SoundManager.SoundEffect.Error); Toast.makeText(requireContext(), R.string.shop_not_found, Toast.LENGTH_SHORT).show() }
                    }
                } }
            }
        }
    }

    private fun render(items: List<InventoryItemEntity>) {
        binding.shopItemsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val card = ItemShopCardBinding.inflate(inflater, binding.shopItemsContainer, false)
            card.root.setBackgroundResource(
                if (item.isUnlocked || item.isSelected) R.drawable.item_bg else R.drawable.item_bg_inactive
            )
            card.artworkImageView.setImageResource(ShopArtwork.previewFor(item.id))
            card.titleTextView.text = item.title
            card.descriptionTextView.text = item.description
            card.actionButton.text = when {
                item.isSelected -> getString(R.string.shop_selected)
                item.isUnlocked -> getString(R.string.shop_equip)
                else -> getString(R.string.shop_buy_format, item.price)
            }
            card.actionButton.isEnabled = !item.isSelected
            card.actionButton.alpha = if (item.isSelected) 0.65f else 1f
            card.actionButton.setOnClickListener { viewModel.buyOrSelect(item.id) }
            binding.shopItemsContainer.addView(card.root)
        }
        if (items.size % 2 != 0) {
            val locked = ItemShopCardBinding.inflate(inflater, binding.shopItemsContainer, false)
            locked.root.setBackgroundResource(R.drawable.item_bg_inactive)
            locked.artworkImageView.setImageResource(R.drawable.ic_lock)
            locked.titleTextView.text = getString(R.string.shop_level_locked_title)
            locked.descriptionTextView.text = getString(R.string.shop_level_locked_description)
            locked.actionButton.text = getString(R.string.shop_level_locked_action)
            locked.actionButton.isEnabled = false
            locked.actionButton.alpha = 0.65f
            binding.shopItemsContainer.addView(locked.root)
        }
    }

    override fun onResume() { super.onResume(); viewModel.load(); soundManager.playMusic(SoundManager.MusicTrack.Menu) }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
