package com.example.flowalbum

/**
 * 图片文件夹数据模型
 * 用于存储文件夹信息和该文件夹下的图片数量
 */
data class PhotoFolder(
    // 文件夹路径
    val path: String,
    
    // 文件夹名称
    val name: String,
    
    // 该文件夹中的图片数量
    val photoCount: Int,
    
    // 文件夹的第一张图片（用作封面）
    val coverPhoto: Photo? = null
) {
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return name.ifEmpty { "未命名文件夹" }
    }
    
    /**
     * 获取格式化的图片数量
     */
    fun getFormattedCount(): String {
        return "$photoCount 张图片"
    }
}