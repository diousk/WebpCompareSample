package com.example.webp

import android.annotation.SuppressLint
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.onAnimationEnd
import coil.request.onAnimationStart
import coil.request.repeatCount
import coil.target.ImageViewTarget
import com.facebook.drawee.view.SimpleDraweeView
import com.show.animated_webp.drawable.WebPDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val imageBuilder: ImageRequest.Builder.() -> Unit by lazy(LazyThreadSafetyMode.NONE) {
        {
            repeatCount(1)
            onAnimationEnd { Timber.d("onAnimationEnd") }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


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
                    Timber.d("play start $drawable")
                    delay(getLoopDurationMs())
                    Timber.d("play end $drawable")
                }
            }
//            imageView.load(R.drawable.tiny, builder = imageBuilder)
        }


        /**
         * Android Api >= 28
         */
        findViewById<Button>(R.id.nativeText).setOnClickListener {
            val ivWebp2 = findViewById<ImageView>(R.id.ivWebp2)
            val animationDrawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.drawable.large))
            val drawable = animationDrawable as AnimatedImageDrawable
            drawable.start()
            ivWebp2.setImageDrawable(drawable)
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