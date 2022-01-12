package com.example.webp

import android.graphics.drawable.Animatable
import android.net.Uri
import androidx.annotation.DrawableRes
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.backend.AnimationBackendDelegate
import com.facebook.fresco.animation.drawable.AnimatedDrawable2
import com.facebook.fresco.animation.drawable.BaseAnimationListener
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import timber.log.Timber
import java.io.File

fun SimpleDraweeView.loadImageUrl(
    imgUrl: String?,
    staticImage: Boolean = false
) {
    if (imgUrl == null) {
        Timber.w("The imgUrl is null. Make actual image transparent")
        setActualImageResource(android.R.color.transparent)
        return
    }

    val cacheChoice = ImageRequest.CacheChoice.DEFAULT

    val request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imgUrl)).apply {
        setResizeOptions(resizeOptions)
        setCacheChoice(cacheChoice)
        if (staticImage) {
            val optionBuilder = ImageDecodeOptionsBuilder<ImageDecodeOptionsBuilder<*>>().apply {
                forceStaticImage = true
            }
            imageDecodeOptions = ImageDecodeOptions(optionBuilder)
        }
    }.build()

    // Load image url.
    controller = Fresco.newDraweeControllerBuilder().apply {
        autoPlayAnimations = !staticImage
        oldController = controller
        imageRequest = request
    }.build()
}

fun SimpleDraweeView.loadImgFile(filePath: String?) {
    if (filePath == null) {
        Timber.d("The file path is null. make actual image transparent")
        setActualImageResource(android.R.color.transparent)
        return
    }


    val request = ImageRequestBuilder
        .newBuilderWithSource(Uri.fromFile(File(filePath)))
        .build()

    // Load image
    controller = Fresco.newDraweeControllerBuilder().apply {
        autoPlayAnimations = true
        oldController = controller
        imageRequest = request
    }.build()
}

fun SimpleDraweeView.loadLocalAnimatedResource(
    @DrawableRes resourceId: Int,
    autoPlay: Boolean = true,
    loops: Boolean = true,
    onStopped: () -> Unit = {}
) {
    val request = ImageRequestBuilder.newBuilderWithResourceId(resourceId).build()
    val listener = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(id: String, imageInfo: ImageInfo?, animatable: Animatable?) {
            Timber.d("onFinalImageSet $animatable")
            (animatable as? AnimatedDrawable2)?.apply {
                if (!loops) {
                    animationBackend = LoopBackend(animationBackend)
                }

                setAnimationListener(
                    object : BaseAnimationListener() {
                        override fun onAnimationStart(drawable: AnimatedDrawable2?) {
                            Timber.d("onAnimationStart ${drawable?.loopDurationMs}")
                            super.onAnimationStart(drawable)
                        }

                        override fun onAnimationStop(drawable: AnimatedDrawable2?) {
                            Timber.d("onAnimationStop $animatable")
                            super.onAnimationStop(drawable)
                            onStopped.invoke()
                        }
                    }
                )
            }
        }

        override fun onFailure(id: String, throwable: Throwable) {
            Timber.e(throwable)
        }
    }

    controller = Fresco.newDraweeControllerBuilder().apply {
        setUri(request.sourceUri)
        controllerListener = listener
        autoPlayAnimations = autoPlay
        oldController = controller
        imageRequest = request
    }.build()
}

class LoopBackend(
    animBackend: AnimationBackend?,
    private val loopsCount: Int = 1
) : AnimationBackendDelegate<AnimationBackend>(animBackend) {

    override fun getLoopCount(): Int {
        return loopsCount
    }
}