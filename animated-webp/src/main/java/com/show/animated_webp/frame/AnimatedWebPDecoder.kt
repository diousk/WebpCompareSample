package com.show.animated_webp.frame

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import com.show.animated_webp.decoder.WebPSupportStatus
import com.show.animated_webp.drawable.WebPDrawable
import com.show.animated_webp.io.ByteBufferLoader
import kotlinx.coroutines.runInterruptible
import java.nio.ByteBuffer

class AnimatedWebPDecoder constructor(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {


    override suspend fun decode() = runInterruptible {
        val byteArray = source.source().readByteArray()
        val byteBuffer = ByteBuffer.allocateDirect(byteArray.size).put(byteArray)
        val drawable = WebPDrawable(object :
            ByteBufferLoader() {
            override fun getByteBuffer(): ByteBuffer {
                byteBuffer.position(0)
                return byteBuffer
            }
        })
        drawable.setLoopLimit(options.parameters.repeatCount() ?: 0)
        val onStart = options.parameters.animationStartCallback()
        val onEnd = options.parameters.animationEndCallback()
        drawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationStart(drawable: Drawable?) {
                super.onAnimationStart(drawable)
                onStart?.invoke()
            }

            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                onEnd?.invoke()
            }
        })

        DecodeResult(drawable, false)
    }


    class Factory : Decoder.Factory {

        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            val headerBytes =
                result.source.source().peek().readByteArray(WebPSupportStatus.HEADER_SIZE)
            if (!(WebPSupportStatus.isWebpHeader(headerBytes, 0, headerBytes.size) &&
                    WebPSupportStatus.isAnimatedWebpHeader(headerBytes, 0))
            ) return null

            return AnimatedWebPDecoder(result.source, options)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

}