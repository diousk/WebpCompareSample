package com.example.webp

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.onAnimationEnd
import coil.request.onAnimationStart
import coil.request.repeatCount
import coil.target.ImageViewTarget
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.show.animated_webp.drawable.WebPDrawable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageModule {
    @Binds
    abstract fun bindPrefetch(impl: ImagePrefetchImpl): ImagePrefetch
}

interface ImagePrefetch {
    suspend fun toFresco(url: String?)
    suspend fun toCoil(url: String?)
}

class ImagePrefetchImpl @Inject constructor(
    @ApplicationContext val context: Context
) : ImagePrefetch {
    override suspend fun toFresco(url: String?) = suspendCancellableCoroutine<Unit> { cont ->
        if (url.isNullOrEmpty()) {
            Timber.w("url null, ignore")
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val imagePipeline = Fresco.getImagePipeline()
        val request = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(url))
            .build()

        val dataSource = imagePipeline.prefetchToDiskCache(request, null, Priority.HIGH)
        dataSource.subscribe(
            object : BaseDataSubscriber<Void>() {
                override fun onFailureImpl(dataSource: DataSource<Void>) {
                    Timber.w("fresco prefetch failed")
                    if (cont.isActive) {
                        cont.resumeWithException(dataSource.failureCause ?: Throwable("unknown error"))
                    }
                }

                override fun onNewResultImpl(dataSource: DataSource<Void>) {
                    Timber.d("fresco prefetch success")
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
            },
            CallerThreadExecutor.getInstance()
        )
        cont.invokeOnCancellation { dataSource.close() }
    }

    override suspend fun toCoil(url: String?) {
        if (url.isNullOrEmpty()) {
            Timber.w("url null, ignore")
            return
        }

        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        val result = context.imageLoader.execute(request)
        Timber.d("result duration ${(result.drawable as WebPDrawable).getLoopDurationMs()}")
    }
}