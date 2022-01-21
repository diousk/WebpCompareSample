package com.example.webp

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.webp.databinding.ViewSmallGiftBinding
import com.example.webp.ext.awaitAnimationEnd
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmallGiftView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    @Inject
    lateinit var activity: Lazy<FragmentActivity>
    private val lifecycleScope by lazy(LazyThreadSafetyMode.NONE) { activity.get().lifecycleScope }
    private val binding = ViewSmallGiftBinding.inflate(LayoutInflater.from(context), this, true)

    private val displayChannel = Channel<Gift>()

    init {
        lifecycleScope.launchWhenResumed {
            displayChannel.consumeEach {
                Timber.d("display gift $it")
                binding.giftView.isVisible = true
                binding.giftView.loadImageUrl(it.url, staticImage = true)
                binding.giftView.awaitAnimationEnd(fadeInAnimation())
                binding.giftView.isVisible = false
            }
        }
    }

    fun enqueueGift(gift: Gift) {
        lifecycleScope.launchWhenResumed {
            Timber.d("enqueueGift gift $gift")
            displayChannel.send(gift)
        }
    }

    private fun fadeInAnimation() = AnimationUtils.loadAnimation(context, R.anim.anim_small_gift).apply {
        fillAfter = true
    }
}