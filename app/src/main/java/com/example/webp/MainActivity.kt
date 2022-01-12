package com.example.webp

import android.annotation.SuppressLint
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.request.ImageRequestBuilder
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainActivity : AppCompatActivity() {
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
            imageView.load(R.drawable.large)
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