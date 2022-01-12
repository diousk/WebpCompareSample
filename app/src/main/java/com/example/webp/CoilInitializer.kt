package com.example.webp

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import com.show.animated_webp.frame.AnimatedWebPDecoder
import kotlinx.coroutines.newFixedThreadPoolContext
import okhttp3.OkHttpClient

object CoilInitializer {
    fun init(application: Application) {
        // set maximum threads for coil image loader to improve scrolling performance
        val dispatchers = newFixedThreadPoolContext(3, "coil_image_thread")

        val context = application.applicationContext
        val okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(MemoryCache.Builder(context).maxSizePercent(0.25).build())
            .components {
                add(AnimatedWebPDecoder.Factory())
            }
            .dispatcher(dispatchers)
            .crossfade(true)
            .okHttpClient(okHttpClient)
            .build()

        Coil.setImageLoader(imageLoader)
    }
}