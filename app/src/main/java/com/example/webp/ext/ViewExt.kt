package com.example.webp.ext

import android.view.View
import android.view.animation.Animation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun View.awaitAnimationEnd(anim: Animation) {
    return suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { clearAnimation() }

        anim.setAnimationListener(
            object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            }
        )
        startAnimation(anim)
    }
}