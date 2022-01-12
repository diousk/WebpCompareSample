package com.example.webp

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.disk.NoOpDiskTrimmableRegistry
import com.facebook.common.internal.Supplier
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.core.ImagePipelineFactory
import okhttp3.OkHttpClient
import kotlin.math.min

object ByteConst {
    const val BYTE = 1
    const val KB = 1024
    const val MB = 1024 * KB
    const val GB = 1024 * KB * KB
}


object FrescoInitializer {

    fun init(application: Application) {
        // setup low memory listener
        application.registerComponentCallbacks(componentCallback())

        val diskCacheConfig = DiskCacheConfig.newBuilder(application)
            .setMaxCacheSize(100 * ByteConst.MB.toLong())
            .setMaxCacheSizeOnLowDiskSpace(60 * ByteConst.MB.toLong())
            .setMaxCacheSizeOnVeryLowDiskSpace(20 * ByteConst.MB.toLong())
            .setDiskTrimmableRegistry(NoOpDiskTrimmableRegistry.getInstance())
            .build()

        val smallImageDiskCacheConfig = DiskCacheConfig.newBuilder(application)
            .setMaxCacheSize((20 * ByteConst.MB).toLong())
            .setMaxCacheSizeOnLowDiskSpace((12 * ByteConst.MB).toLong())
            .setMaxCacheSizeOnVeryLowDiskSpace((4 * ByteConst.MB).toLong())
            .setDiskTrimmableRegistry(NoOpDiskTrimmableRegistry.getInstance())
            .build()

        val trimRegistry = memoryTrimRegistry()

        val okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
        val config = OkHttpImagePipelineConfigFactory.newBuilder(application, okHttpClient)
            .setDownsampleEnabled(true)
            .setResizeAndRotateEnabledForNetwork(true)
            .setBitmapsConfig(Bitmap.Config.RGB_565)
            .setMainDiskCacheConfig(diskCacheConfig)
            .setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
            .setBitmapMemoryCacheParamsSupplier(
                FrescoMemoryCacheSupplier(
                    application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                )
            )
            .setMemoryTrimmableRegistry(trimRegistry)
            .build()

        Fresco.initialize(application, config)
    }

    private fun memoryTrimRegistry(): MemoryTrimmableRegistry {

        val memoryTrimmableRegistry = NoOpMemoryTrimmableRegistry.getInstance()

        memoryTrimmableRegistry.registerMemoryTrimmable { trimType ->

            when (trimType.suggestedTrimRatio) {
                MemoryTrimType.OnCloseToDalvikHeapLimit.suggestedTrimRatio,
                MemoryTrimType.OnSystemLowMemoryWhileAppInBackground.suggestedTrimRatio,
                MemoryTrimType.OnSystemLowMemoryWhileAppInForeground.suggestedTrimRatio ->
                    clearMemoryCaches()
            }
        }

        return memoryTrimmableRegistry
    }

    private fun componentCallback(): ComponentCallbacks2 {

        return object : ComponentCallbacks2 {

            override fun onLowMemory() {
                clearMemoryCaches()
                System.gc()
            }

            override fun onConfigurationChanged(newConfig: Configuration) {
            }

            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                    clearMemoryCaches()
                }
            }
        }
    }

    fun clearMemoryCaches() {
        try {
            ImagePipelineFactory.getInstance().imagePipeline.clearMemoryCaches()
        } catch (e: Exception) {
        }
    }
}

class FrescoMemoryCacheSupplier(private val activityManager: ActivityManager) : Supplier<MemoryCacheParams> {
    private val maxCacheSize: Int
        get() {
            val maxMemory = min(activityManager.memoryClass * ByteConst.MB, Integer.MAX_VALUE)
            return when {
                maxMemory < 32 * ByteConst.MB -> 4 * ByteConst.MB
                maxMemory < 64 * ByteConst.MB -> 6 * ByteConst.MB
                else -> 16 * ByteConst.MB
            }
        }

    override fun get(): MemoryCacheParams {

        return MemoryCacheParams(
            maxCacheSize,
            MAX_CACHE_ENTRIES,
            maxCacheSize,
            MAX_CACHE_EVICTION_ENTRIES,
            maxCacheSize
        )
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 64
        private const val MAX_CACHE_EVICTION_ENTRIES = 10
    }
}
