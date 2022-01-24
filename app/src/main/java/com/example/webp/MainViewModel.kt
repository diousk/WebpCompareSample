package com.example.webp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imagePrefetch: ImagePrefetch
) : ViewModel() {
    enum class Loader { Fresco, Coil }

    val imageLoader = Loader.Coil

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

    private val numServants = 3 // for prepare gift resource concurrently

    private val giftEventChannel = Channel<Gift>()

    private val selfResultChannel = Channel<Gift>(Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val otherResultChannel = Channel<Gift>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val smallResultChannel = Channel<Gift>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch {
            val servants = (0 until numServants).map { serveGiftEvent(giftEventChannel, "chnl-$it") }
            fanIn(servants).consumeEach {
                Timber.d("gift processed, type ${it.type}, self ${it.isSelf}")
                when (it.type) {
                    is Type.Big -> {
                        if (it.isSelf) {
                            selfResultChannel.send(it)
                        } else {
                            otherResultChannel.send(it)
                        }
                    }
                    is Type.Small -> {
                        smallResultChannel.send(it)
                    }
                }
            }
        }
    }

    fun observeBigGift(): ReceiveChannel<Gift> = viewModelScope.produce {
        while (isActive) {
            select<Unit> {
                selfResultChannel.onReceive {
                    Timber.d("receive my gift")
                    send(it)
                }
                otherResultChannel.onReceive {
                    Timber.d("receive other gift")
                    send(it)
                }
            }
        }
    }

    private suspend fun prepareGiftResource(gift: Gift): Gift {
        val random = Random.nextLong(1000)
        Timber.d("prepare random $random")
        delay(random)
        runCatching {
            withTimeout(120_000L) {
                Timber.d("cache image to $imageLoader")
                if (imageLoader == Loader.Fresco) {
                    imagePrefetch.toFresco(gift.url)
                } else {
                    imagePrefetch.toCoil(gift.url)
                }
                Timber.d("cache image done $imageLoader")
            }
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
    fun sendSelfBigGift() {
        val url = "http://blob.ufile.ucloud.com.cn/e0722a92365239c43afd3c7307c1f6ec"
        viewModelScope.launch {
            giftEventChannel.send(Gift(Type.Big, url, isSelf = true))
        }
    }

    fun cancel() {
        viewModelScope.cancel()
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