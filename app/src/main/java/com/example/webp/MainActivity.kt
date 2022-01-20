package com.example.webp

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.flow.collect
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

        findViewById<Button>(R.id.smallGift).setOnClickListener {
            viewModel.sendSmallGift(0)
        }
        findViewById<Button>(R.id.big1).setOnClickListener {
            viewModel.sendBigGift1()
        }
        findViewById<Button>(R.id.big2).setOnClickListener {
            viewModel.sendBigGift2()
        }
        findViewById<Button>(R.id.big3).setOnClickListener {
            viewModel.sendBigGift3()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.resultChannel.consumeEach {
                Timber.d("resultFlow gift $it")
                delay(3000)
                Timber.d("resultFlow done gift $it")
            }
        }



        val smallWebp = "http://blob.ufile.ucloud.com.cn/09ec18187db5ecaee28d3e49ec524f67"
        val largeWebp = "http://blob.ufile.ucloud.com.cn/dwt1_43000MS.webp"
        /**
         * Load it by Coil's custom Decoder
         */
        findViewById<Button>(R.id.coilText).setOnClickListener {
            val imageView = findViewById<ImageView>(R.id.ivWebp)
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
                Timber.d("start execute $drawable")
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
            val ivWebp3 = findViewById<SimpleDraweeView>(R.id.ivWebp3)
            ivWebp3.loadLocalAnimatedResource(R.drawable.tiny)
        }
    }
}