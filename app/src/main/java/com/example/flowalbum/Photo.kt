package com.example.flowalbum

import android.net.Uri

/**
 * 图片数据模型类
 * 用于存储单张图片的信息
 */
data class Photo(
    // 图片文件的URI（统一资源标识符）
    val uri: Uri,
    
    // 图片文件路径
    val path: String,
    
    // 图片文件名
    val name: String,
    
    // 图片文件大小（字节）
    val size: Long = 0,
    
    // 图片添加时间戳
    val dateAdded: Long = 0,
    
    // 图片修改时间戳
    val dateModified: Long = 0
) {
    /**
     * 判断图片文件是否存在
     */
    fun exists(): Boolean {
        return path.isNotEmpty()
    }
    
    /**
     * 获取图片的显示名称（用于UI显示）
     */
    fun getDisplayName(): String {
        return name.ifEmpty { "未知图片" }
    }
    
    /**
     * 获取格式化的文件大小
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}