package com.example.flowalbum

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

// A map of transformation names to PageTransformer objects.
val transformationMap: Map<String, ViewPager2.PageTransformer> = mapOf(
    "None" to ViewPager2.PageTransformer { _, _ -> },
    "Depth" to DepthPageTransformer(),
    "Zoom Out" to ZoomOutPageTransformer(),
    "Fade Out" to FadeOutPageTransformer()
)

class DepthPageTransformer : ViewPager2.PageTransformer {
    private val minScale = 0.75f

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    alpha = 1f
                    translationX = 0f
                    translationZ = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    alpha = 1 - position
                    translationX = pageWidth * -position
                    translationZ = -1f
                    val scaleFactor = (minScale + (1 - minScale) * (1 - abs(position)))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    alpha = 0f
                }
            }
        }
    }
}

class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    private val minScale = 0.85f
    private val minAlpha = 0.5f

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    val scaleFactor = minScale.coerceAtLeast(1 - abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    alpha = (minAlpha + (((scaleFactor - minScale) / (1 - minScale)) * (1 - minAlpha)))
                }
                else -> { // (1,+Infinity]
                    alpha = 0f
                }
            }
        }
    }
}

class FadeOutPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.translationX = -position * view.width
        view.alpha = 1 - abs(position)
    }
}