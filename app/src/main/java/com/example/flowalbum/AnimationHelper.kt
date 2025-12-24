package com.example.flowalbum

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.*
import kotlin.random.Random

/**
 * 动画工具类
 * 提供多种图片切换动画效果
 */
class AnimationHelper {

    companion object {
        // 动画持续时间（毫秒）
        private const val ANIMATION_DURATION = 800L
    }

    /**
     * 动画类型枚举
     */
    enum class AnimationType {
        FADE,           // 淡入淡出
        SLIDE_LEFT,     // 左滑
        SLIDE_RIGHT,    // 右滑
        SLIDE_UP,       // 上滑
        SLIDE_DOWN,     // 下滑
        ZOOM_IN,        // 放大
        ZOOM_OUT,       // 缩小
        ROTATE,         // 旋转
        RANDOM          // 随机效果
    }

    /**
     * 应用动画效果到视图
     * @param view 目标视图
     * @param type 动画类型
     * @param isEnter true表示进入动画，false表示退出动画
     * @param onComplete 动画完成回调
     */
    fun applyAnimation(
        view: View,
        type: AnimationType,
        isEnter: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        // 如果是随机类型，随机选择一个具体动画
        val actualType = if (type == AnimationType.RANDOM) {
            AnimationType.values().filter { it != AnimationType.RANDOM }.random()
        } else {
            type
        }

        // 根据动画类型执行相应动画
        when (actualType) {
            AnimationType.FADE -> applyFadeAnimation(view, isEnter, onComplete)
            AnimationType.SLIDE_LEFT -> applySlideAnimation(view, isEnter, -1f, 0f, onComplete)
            AnimationType.SLIDE_RIGHT -> applySlideAnimation(view, isEnter, 1f, 0f, onComplete)
            AnimationType.SLIDE_UP -> applySlideAnimation(view, isEnter, 0f, -1f, onComplete)
            AnimationType.SLIDE_DOWN -> applySlideAnimation(view, isEnter, 0f, 1f, onComplete)
            AnimationType.ZOOM_IN -> applyZoomAnimation(view, isEnter, true, onComplete)
            AnimationType.ZOOM_OUT -> applyZoomAnimation(view, isEnter, false, onComplete)
            AnimationType.ROTATE -> applyRotateAnimation(view, isEnter, onComplete)
            else -> onComplete?.invoke()
        }
    }

    /**
     * 淡入淡出动画
     */
    private fun applyFadeAnimation(
        view: View,
        isEnter: Boolean,
        onComplete: (() -> Unit)?
    ) {
        val startAlpha = if (isEnter) 0f else 1f
        val endAlpha = if (isEnter) 1f else 0f

        view.alpha = startAlpha
        view.animate()
            .alpha(endAlpha)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }

    /**
     * 滑动动画
     * @param directionX X方向（-1=左, 0=无, 1=右）
     * @param directionY Y方向（-1=上, 0=无, 1=下）
     */
    private fun applySlideAnimation(
        view: View,
        isEnter: Boolean,
        directionX: Float,
        directionY: Float,
        onComplete: (() -> Unit)?
    ) {
        val parent = view.parent as? View ?: return
        
        // 计算起始和结束位置
        val startX = if (isEnter) parent.width * directionX else 0f
        val endX = if (isEnter) 0f else parent.width * directionX
        val startY = if (isEnter) parent.height * directionY else 0f
        val endY = if (isEnter) 0f else parent.height * directionY

        view.translationX = startX
        view.translationY = startY
        view.alpha = if (isEnter) 0f else 1f

        view.animate()
            .translationX(endX)
            .translationY(endY)
            .alpha(if (isEnter) 1f else 0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }

    /**
     * 缩放动画
     * @param zoomIn true表示放大效果，false表示缩小效果
     */
    private fun applyZoomAnimation(
        view: View,
        isEnter: Boolean,
        zoomIn: Boolean,
        onComplete: (() -> Unit)?
    ) {
        val startScale = when {
            isEnter && zoomIn -> 0.5f
            isEnter && !zoomIn -> 1.5f
            !isEnter && zoomIn -> 1f
            else -> 1f
        }
        
        val endScale = when {
            isEnter -> 1f
            zoomIn -> 1.5f
            else -> 0.5f
        }

        view.scaleX = startScale
        view.scaleY = startScale
        view.alpha = if (isEnter) 0f else 1f

        view.animate()
            .scaleX(endScale)
            .scaleY(endScale)
            .alpha(if (isEnter) 1f else 0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }

    /**
     * 旋转动画
     */
    private fun applyRotateAnimation(
        view: View,
        isEnter: Boolean,
        onComplete: (() -> Unit)?
    ) {
        val startRotation = if (isEnter) -90f else 0f
        val endRotation = if (isEnter) 0f else 90f
        val startAlpha = if (isEnter) 0f else 1f
        val endAlpha = if (isEnter) 1f else 0f

        view.rotation = startRotation
        view.alpha = startAlpha

        view.animate()
            .rotation(endRotation)
            .alpha(endAlpha)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            .start()
    }

    /**
     * 交叉淡入淡出动画（用于两个视图之间切换）
     * @param outView 淡出的视图
     * @param inView 淡入的视图
     * @param onComplete 完成回调
     */
    fun crossFadeAnimation(
        outView: View,
        inView: View,
        onComplete: (() -> Unit)? = null
    ) {
        // 淡出旧视图
        outView.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION / 2)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    outView.visibility = View.GONE
                    
                    // 淡入新视图
                    inView.alpha = 0f
                    inView.visibility = View.VISIBLE
                    inView.animate()
                        .alpha(1f)
                        .setDuration(ANIMATION_DURATION / 2)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                onComplete?.invoke()
                            }
                        })
                        .start()
                }
            })
            .start()
    }

    /**
     * 获取所有可用的动画类型（不包括RANDOM）
     */
    fun getAvailableAnimations(): List<AnimationType> {
        return AnimationType.values().filter { it != AnimationType.RANDOM }
    }

    /**
     * 获取动画类型的显示名称
     */
    fun getAnimationName(type: AnimationType): String {
        return when (type) {
            AnimationType.FADE -> "淡入淡出"
            AnimationType.SLIDE_LEFT -> "左滑"
            AnimationType.SLIDE_RIGHT -> "右滑"
            AnimationType.SLIDE_UP -> "上滑"
            AnimationType.SLIDE_DOWN -> "下滑"
            AnimationType.ZOOM_IN -> "放大"
            AnimationType.ZOOM_OUT -> "缩小"
            AnimationType.ROTATE -> "旋转"
            AnimationType.RANDOM -> "随机效果"
        }
    }
}