package com.example.webp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imagePrefetch: ImagePrefetch
) : ViewModel() {

    // Note: return type can be different
    fun CoroutineScope.serveGiftEvent(
        giftEventChannel: ReceiveChannel<Gift>,
        name: String
    ): ReceiveChannel<Gift> = produce {
        for (gift in giftEventChannel) {
            Timber.d("$name process gift $gift")
            val giftResult = prepareGiftResource(gift)
            send(giftResult)
        }
    }

    private val maxServants = 2 // for prepare gift resource concurrently

    private val giftEventChannel = Channel<Gift>()

    private val selfResultChannel = Channel<Gift>(Channel.BUFFERED)
    private val otherResultChannel = Channel<Gift>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resultChannel = Channel<Gift>()

    init {
        viewModelScope.launch {
            val servants = (0..maxServants).map { serveGiftEvent(giftEventChannel, "chnl-$it") }
            fanIn(servants).consumeEach {
                Timber.d("gift processed $it")
                if (it.isSelf) {
                    selfResultChannel.send(it)
                } else {
                    otherResultChannel.send(it)
                }
            }
        }
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                val gift = select<Gift> {
                    selfResultChannel.onReceive {
                        Timber.d("receive my gift")
                        it
                    }
                    otherResultChannel.onReceive {
                        Timber.d("receive other gift")
                        it
                    }
                }
                resultChannel.send(gift)
            }
        }

    }

    private suspend fun prepareGiftResource(gift: Gift): Gift {
        runCatching {
            withTimeout(10_000L) { imagePrefetch.toFresco(gift.url) }
        }
        return gift
    }

    // simulate socket event from remote server
    fun sendSmallGift(position: Int) {
        val clap = "https://playone-assets.staging.baulong.club/playone/gift/c9ac382d8466f583a6e87218ac5a949b.png"
        viewModelScope.launch {
            giftEventChannel.send(Gift(Type.Small(position), clap, isSelf = false))
        }
    }

    // simulate socket event from remote server
    fun sendBigGift1() {
        val url = "http://blob.ufile.ucloud.com.cn/09ec18187db5ecaee28d3e49ec524f67"
        viewModelScope.launch {
            giftEventChannel.send(Gift(Type.Big, url, isSelf = false))
        }
    }

    // simulate socket event from remote server
    fun sendBigGift2() {
        val url = "http://blob.ufile.ucloud.com.cn/dwt1_43000MS.webp"
        viewModelScope.launch {
            giftEventChannel.send(Gift(Type.Big, url, isSelf = false))
        }
    }

    // simulate socket event from remote server
    fun sendBigGift3() {
        val url = "http://blob.ufile.ucloud.com.cn/e0722a92365239c43afd3c7307c1f6ec"
        viewModelScope.launch {
            giftEventChannel.send(Gift(Type.Big, url, isSelf = true))
        }
    }
}

fun <T> CoroutineScope.fanIn(
    channels: List<ReceiveChannel<T>>
): ReceiveChannel<T> = produce {
    for (channel in channels) {
        launch {
            for (elem in channel) {
                send(elem)
            }
        }
    }
}