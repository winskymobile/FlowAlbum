package com.example.flowalbum

import android.graphics.Bitmap
import android.graphics.Color
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

/**
 * Glide图片锐化变换
 * 使用卷积矩阵实现真正的锐化效果
 */
class SharpnessTransformation(private val sharpnessLevel: Int) : BitmapTransformation() {

    companion object {
        private const val ID = "com.example.flowalbum.SharpnessTransformation"
        
        // 锐化卷积核 - 3x3矩阵
        // 中心值越大，锐化效果越强
        private fun getSharpenKernel(level: Int): FloatArray {
            return when (level) {
                1 -> floatArrayOf(
                    0f, -0.5f, 0f,
                    -0.5f, 3f, -0.5f,
                    0f, -0.5f, 0f
                )
                2 -> floatArrayOf(
                    0f, -1f, 0f,
                    -1f, 5f, -1f,
                    0f, -1f, 0f
                )
                3 -> floatArrayOf(
                    -0.5f, -1f, -0.5f,
                    -1f, 7f, -1f,
                    -0.5f, -1f, -0.5f
                )
                4 -> floatArrayOf(
                    -1f, -1f, -1f,
                    -1f, 9f, -1f,
                    -1f, -1f, -1f
                )
                5 -> floatArrayOf(
                    -1f, -2f, -1f,
                    -2f, 13f, -2f,
                    -1f, -2f, -1f
                )
                else -> floatArrayOf(
                    0f, 0f, 0f,
                    0f, 1f, 0f,
                    0f, 0f, 0f
                )
            }
        }
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // 如果锐化等级为0，直接返回原图
        if (sharpnessLevel <= 0) {
            return toTransform
        }

        val width = toTransform.width
        val height = toTransform.height

        // 从池中获取或创建输出Bitmap
        val result = pool.get(width, height, Bitmap.Config.ARGB_8888)
        
        // 获取锐化卷积核
        val kernel = getSharpenKernel(sharpnessLevel)
        
        // 应用卷积
        applyConvolution(toTransform, result, kernel)
        
        return result
    }

    /**
     * 应用3x3卷积矩阵
     */
    private fun applyConvolution(source: Bitmap, dest: Bitmap, kernel: FloatArray) {
        val width = source.width
        val height = source.height
        
        // 获取所有像素
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val resultPixels = IntArray(width * height)
        
        // 对每个像素应用卷积（跳过边缘像素）
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f
                var g = 0f
                var b = 0f
                
                // 3x3卷积
                var kernelIndex = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[kernelIndex++]
                        
                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }
                
                // 限制颜色值在0-255范围内
                val finalR = clamp(r.toInt())
                val finalG = clamp(g.toInt())
                val finalB = clamp(b.toInt())
                
                // 保持原始alpha值
                val alpha = Color.alpha(pixels[y * width + x])
                resultPixels[y * width + x] = Color.argb(alpha, finalR, finalG, finalB)
            }
        }
        
        // 复制边缘像素（不处理）
        for (x in 0 until width) {
            resultPixels[x] = pixels[x]  // 第一行
            resultPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]  // 最后一行
        }
        for (y in 0 until height) {
            resultPixels[y * width] = pixels[y * width]  // 第一列
            resultPixels[y * width + width - 1] = pixels[y * width + width - 1]  // 最后一列
        }
        
        // 设置结果像素
        dest.setPixels(resultPixels, 0, width, 0, 0, width, height)
    }
    
    private fun clamp(value: Int): Int {
        return max(0, min(255, value))
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + sharpnessLevel).toByteArray(Charsets.UTF_8))
    }

    override fun equals(other: Any?): Boolean {
        return other is SharpnessTransformation && other.sharpnessLevel == sharpnessLevel
    }

    override fun hashCode(): Int {
        return ID.hashCode() + sharpnessLevel
    }
}