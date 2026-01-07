package com.example.flowalbum

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理类
 * 用于保存和读取用户的偏好设置
 */
class SettingsManager(context: Context) {

    companion object {
        // SharedPreferences文件名
        private const val PREFS_NAME = "FlowAlbumSettings"
        
        // 设置键名
        private const val KEY_INTERVAL = "slideshow_interval"
        private const val KEY_ANIMATION_TYPE = "animation_type"
        private const val KEY_RANDOM_ANIMATION = "random_animation"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_HIGH_QUALITY = "high_quality_mode"
        private const val KEY_HARDWARE_ACCEL = "hardware_acceleration"
        private const val KEY_FIT_SCREEN = "fit_screen"
        private const val KEY_COLOR_SATURATION = "color_saturation"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_SKIPPED_VERSION = "skipped_update_version"
        private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"
        
        // 默认值
        const val DEFAULT_INTERVAL = 3000L // 默认3秒切换
        const val MIN_INTERVAL = 1000L     // 最小1秒
        const val MAX_INTERVAL = 30000L    // 最大30秒
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取图片切换间隔（毫秒）
     */
    fun getInterval(): Long {
        return prefs.getLong(KEY_INTERVAL, DEFAULT_INTERVAL)
    }

    /**
     * 设置图片切换间隔（毫秒）
     */
    fun setInterval(interval: Long) {
        // 确保间隔在有效范围内
        val validInterval = interval.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        prefs.edit().putLong(KEY_INTERVAL, validInterval).apply()
    }

    /**
     * 获取动画类型
     * 返回AnimationType的序号
     */
    fun getAnimationType(): Int {
        return prefs.getInt(KEY_ANIMATION_TYPE, AnimationHelper.AnimationType.FADE.ordinal)
    }

    /**
     * 设置动画类型
     */
    fun setAnimationType(type: AnimationHelper.AnimationType) {
        prefs.edit().putInt(KEY_ANIMATION_TYPE, type.ordinal).apply()
    }

    /**
     * 获取动画类型枚举
     */
    fun getAnimationTypeEnum(): AnimationHelper.AnimationType {
        val ordinal = getAnimationType()
        return AnimationHelper.AnimationType.values().getOrNull(ordinal) 
            ?: AnimationHelper.AnimationType.FADE
    }

    /**
     * 是否使用随机动画
     */
    fun isRandomAnimation(): Boolean {
        return prefs.getBoolean(KEY_RANDOM_ANIMATION, false)
    }

    /**
     * 设置是否使用随机动画
     */
    fun setRandomAnimation(random: Boolean) {
        prefs.edit().putBoolean(KEY_RANDOM_ANIMATION, random).apply()
    }

    /**
     * 获取图片文件夹路径
     */
    fun getFolderPath(): String? {
        return prefs.getString(KEY_FOLDER_PATH, null)
    }

    /**
     * 设置图片文件夹路径
     */
    fun setFolderPath(path: String?) {
        prefs.edit().putString(KEY_FOLDER_PATH, path).apply()
    }

    /**
     * 是否自动播放
     */
    fun isAutoPlay(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PLAY, true)
    }

    /**
     * 设置是否自动播放
     */
    fun setAutoPlay(autoPlay: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, autoPlay).apply()
    }

    /**
     * 获取间隔秒数（用于UI显示）
     */
    fun getIntervalSeconds(): Int {
        return (getInterval() / 1000).toInt()
    }

    /**
     * 通过秒数设置间隔
     */
    fun setIntervalSeconds(seconds: Int) {
        setInterval(seconds * 1000L)
    }

    /**
     * 是否启用高质量模式
     */
    fun isHighQualityMode(): Boolean {
        return prefs.getBoolean(KEY_HIGH_QUALITY, true)
    }

    /**
     * 设置是否启用高质量模式
     */
    fun setHighQualityMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HIGH_QUALITY, enabled).apply()
    }

    /**
     * 是否启用硬件加速
     */
    fun isHardwareAcceleration(): Boolean {
        return prefs.getBoolean(KEY_HARDWARE_ACCEL, true)
    }

    /**
     * 设置是否启用硬件加速
     */
    fun setHardwareAcceleration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HARDWARE_ACCEL, enabled).apply()
    }

    /**
     * 是否启用适应屏幕（缩放至全屏）
     */
    fun isFitScreen(): Boolean {
        return prefs.getBoolean(KEY_FIT_SCREEN, true)
    }

    /**
     * 设置是否启用适应屏幕
     */
    fun setFitScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FIT_SCREEN, enabled).apply()
    }

    /**
     * 获取色彩饱和度（0到5，默认0）
     */
    fun getColorSaturation(): Int {
        return prefs.getInt(KEY_COLOR_SATURATION, 0)
    }

    /**
     * 设置色彩饱和度
     */
    fun setColorSaturation(level: Int) {
        val validLevel = level.coerceIn(0, 5)
        prefs.edit().putInt(KEY_COLOR_SATURATION, validLevel).apply()
    }

    /**
     * 获取主题颜色（0-8，默认0为青绿色）
     */
    fun getThemeColor(): Int {
        return prefs.getInt(KEY_THEME_COLOR, 0)
    }

    /**
     * 设置主题颜色
     */
    fun setThemeColor(themeIndex: Int) {
        val validIndex = themeIndex.coerceIn(0, 8)
        prefs.edit().putInt(KEY_THEME_COLOR, validIndex).apply()
    }

    /**
     * 重置所有设置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * 导出设置为字符串（用于调试或备份）
     */
    fun exportSettings(): String {
        return """
            切换间隔: ${getIntervalSeconds()}秒
            动画类型: ${getAnimationTypeEnum()}
            随机动画: ${isRandomAnimation()}
            自动播放: ${isAutoPlay()}
            高质量模式: ${isHighQualityMode()}
            硬件加速: ${isHardwareAcceleration()}
            适应屏幕: ${isFitScreen()}
            色彩饱和度: ${getColorSaturation()}
            主题颜色: ${getThemeColor()}
            文件夹路径: ${getFolderPath() ?: "未设置"}
        """.trimIndent()
    }

    /**
     * 获取跳过的版本号
     * 返回格式: "version_timestamp" 例如 "1.0.4_20260107103259"
     */
    fun getSkippedVersion(): String? {
        return prefs.getString(KEY_SKIPPED_VERSION, null)
    }

    /**
     * 设置跳过的版本号
     * @param version 版本号，格式: "version_timestamp" 例如 "1.0.4_20260107103259"
     */
    fun setSkippedVersion(version: String?) {
        prefs.edit().putString(KEY_SKIPPED_VERSION, version).apply()
    }

    /**
     * 清除跳过的版本号（当用户选择更新或有新版本时调用）
     */
    fun clearSkippedVersion() {
        prefs.edit().remove(KEY_SKIPPED_VERSION).apply()
    }

    /**
     * 检查指定版本是否是已跳过的版本
     * @param version 版本号
     * @param timestamp 时间戳
     * @return true表示该版本已被用户跳过
     */
    fun isVersionSkipped(version: String, timestamp: String): Boolean {
        val skippedVersion = getSkippedVersion() ?: return false
        val currentVersionKey = "${version}_${timestamp}"
        return skippedVersion == currentVersionKey
    }

    /**
     * 是否启用自动检测更新（默认启用）
     */
    fun isAutoCheckUpdate(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CHECK_UPDATE, true)
    }

    /**
     * 设置是否启用自动检测更新
     */
    fun setAutoCheckUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATE, enabled).apply()
    }
}