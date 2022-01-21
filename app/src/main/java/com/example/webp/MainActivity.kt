package com.example.webp

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.onAnimationEnd
import coil.request.onAnimationStart
import coil.request.repeatCount
import coil.target.ImageViewTarget
import com.facebook.drawee.view.SimpleDraweeView
import com.show.animated_webp.drawable.WebPDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val imageBuilder: ImageRequest.Builder.() -> Unit by lazy(LazyThreadSafetyMode.NONE) {
        {
            repeatCount(1)
            onAnimationEnd { Timber.d("onAnimationEnd") }
        }
    }
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var counter = 0
        findViewById<Button>(R.id.smallGift).setOnClickListener {
            viewModel.sendSmallGift(counter++ % 2)
        }
        findViewById<Button>(R.id.big1).setOnClickListener {
            viewModel.sendBigGift1()
        }
        findViewById<Button>(R.id.big2).setOnClickListener {
            viewModel.sendBigGift2()
        }
        findViewById<Button>(R.id.self).setOnClickListener {
            viewModel.sendSelfBigGift()
        }

        // big gift display
        lifecycleScope.launchWhenResumed {
            viewModel.resultChannel.consumeEach {
                Timber.d("display gift $it")
                if (viewModel.imageLoader == MainViewModel.Loader.Fresco) {
                    val draweeView = findViewById<SimpleDraweeView>(R.id.frescoWebp)
                    draweeView.isVisible = true
                    val duration = draweeView.loadAnim(it.url)
                    Timber.d("duration of gift $duration")
                    delay(duration)
                    draweeView.isInvisible = true
                } else {
                    val imageView = findViewById<ImageView>(R.id.coilWebp)
                    imageView.isVisible = true
                    val request = ImageRequest.Builder(imageView.context)
                        .data(it.url)
                        .repeatCount(1)
                        .target(imageView)
                        .build()
                    Timber.d("start execute")
                    val drawable = imageView.context.imageLoader.execute(request).drawable
                    val duration = (drawable as WebPDrawable).getLoopDurationMs()
                    Timber.d("play start duration $duration")
                    delay(duration)
                    Timber.d("play end $drawable")
                    imageView.isInvisible = true
                }
                Timber.d("display done gift $it")
            }
        }

        // small gift
        lifecycleScope.launchWhenResumed {
            viewModel.smallResultChannel.consumeEach {
                val position = (it.type as Type.Small).position
                val view = findViewById<ViewGroup>(R.id.smallGiftContainer).getChildAt(position) as SmallGiftView
                view.enqueueGift(it)
            }
        }


        val smallWebp = "http://blob.ufile.ucloud.com.cn/09ec18187db5ecaee28d3e49ec524f67"
        val largeWebp = "http://blob.ufile.ucloud.com.cn/dwt1_43000MS.webp"
        /**
         * Load it by Coil's custom Decoder
         */
        findViewById<Button>(R.id.coilText).setOnClickListener {
            val imageView = findViewById<ImageView>(R.id.coilWebp)
            val request = ImageRequest.Builder(imageView.context)
                .data(R.drawable.tiny)
                //.data("http://blob.ufile.ucloud.com.cn/09ec18187db5ecaee28d3e49ec524f67")
                .repeatCount(1)
                .onAnimationEnd { Timber.d("onAnimationEnd") }
                .onAnimationStart { Timber.d("onAnimationStart") }
                .target(object : ImageViewTarget(imageView) {
                    override fun onSuccess(result: Drawable) {
                        Timber.d("onSuccess result $result webp ${result is Animatable}")
                        super.onSuccess(result)
                    }
                })
                .build()
            CoroutineScope(Dispatchers.Main).launch {
                Timber.d("start execute")
                val drawable = imageView.context.imageLoader.execute(request).drawable
                Timber.d("parsed $drawable")
                with(drawable as WebPDrawable) {
                    val duration = getLoopDurationMs()
                    Timber.d("play start duration $duration")
                    delay(duration)
                    Timber.d("play end $drawable")
                }
            }
//            imageView.load(R.drawable.tiny, builder = imageBuilder)
        }

        /**
         * fresco
         * */
        findViewById<Button>(R.id.frescoText).setOnClickListener {
            val ivWebp3 = findViewById<SimpleDraweeView>(R.id.frescoWebp)
            ivWebp3.loadLocalAnimatedResource(R.drawable.tiny)
        }
    }
}