package com.example.flowalbum

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 更新检测器
 * 通过GitHub仓库检测应用更新
 */
class UpdateChecker(private val context: Context) {

    companion object {
        // GitHub API地址，用于获取release目录下的文件列表
        private const val GITHUB_API_URL = 
            "https://api.github.com/repos/winskymobile/FlowAlbum/contents/app/release"
        
        // GitHub Raw下载地址前缀
        private const val GITHUB_RAW_URL = 
            "https://github.com/winskymobile/FlowAlbum/raw/main/app/release/"
        
        // APK文件名正则表达式：FlowAlbum_v{版本号}_{时间戳}.apk
        private val APK_PATTERN = Regex("""FlowAlbum_v([\d.]+)_(\d{14})\.apk""")
        
        // 连接超时时间（毫秒）
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
    }

    /**
     * 更新信息数据类
     */
    data class UpdateInfo(
        val hasUpdate: Boolean,           // 是否有更新
        val latestVersion: String,        // 最新版本号
        val latestTimestamp: String,      // 最新时间戳
        val currentVersion: String,       // 当前版本号
        val currentTimestamp: String,     // 当前构建时间戳
        val downloadUrl: String,          // 下载地址
        val fileName: String,             // 文件名
        val errorMessage: String? = null  // 错误信息
    )

    /**
     * APK信息数据类
     */
    private data class ApkInfo(
        val fileName: String,
        val version: String,
        val timestamp: String,
        val downloadUrl: String
    )

    /**
     * 检测更新
     * @return UpdateInfo 更新信息
     */
    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            // 获取当前应用版本信息
            val currentVersion = getCurrentVersion()
            val currentTimestamp = getCurrentBuildTimestamp()
            
            // 从GitHub获取APK列表
            val apkList = fetchApkListFromGitHub()
            
            if (apkList.isEmpty()) {
                return@withContext UpdateInfo(
                    hasUpdate = false,
                    latestVersion = currentVersion,
                    latestTimestamp = currentTimestamp,
                    currentVersion = currentVersion,
                    currentTimestamp = currentTimestamp,
                    downloadUrl = "",
                    fileName = "",
                    errorMessage = "未找到可用的更新文件"
                )
            }
            
            // 找到最新的APK
            val latestApk = findLatestApk(apkList)
            
            // 比较版本
            val hasUpdate = isNewerVersion(
                latestApk.version, 
                latestApk.timestamp, 
                currentVersion, 
                currentTimestamp
            )
            
            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = latestApk.version,
                latestTimestamp = latestApk.timestamp,
                currentVersion = currentVersion,
                currentTimestamp = currentTimestamp,
                downloadUrl = latestApk.downloadUrl,
                fileName = latestApk.fileName
            )
        } catch (e: Exception) {
            val currentVersion = getCurrentVersion()
            val currentTimestamp = getCurrentBuildTimestamp()
            
            UpdateInfo(
                hasUpdate = false,
                latestVersion = currentVersion,
                latestTimestamp = currentTimestamp,
                currentVersion = currentVersion,
                currentTimestamp = currentTimestamp,
                downloadUrl = "",
                fileName = "",
                errorMessage = "检测更新失败: ${e.message}"
            )
        }
    }

    /**
     * 获取当前应用版本号
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName, 
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * 获取当前应用构建时间戳
     * 从APK文件名或构建配置中获取
     */
    private fun getCurrentBuildTimestamp(): String {
        // 返回一个默认时间戳，实际应用中可以通过BuildConfig获取
        // 这里使用空字符串表示未知时间戳
        return try {
            // 尝试从包信息获取最后更新时间
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            // 使用lastUpdateTime作为参考时间戳
            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            sdf.format(packageInfo.lastUpdateTime)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 从GitHub API获取APK文件列表
     */
    private fun fetchApkListFromGitHub(): List<ApkInfo> {
        val url = URL(GITHUB_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "FlowAlbum-Android")
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP错误: $responseCode")
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseGitHubResponse(response)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 解析GitHub API响应
     */
    private fun parseGitHubResponse(response: String): List<ApkInfo> {
        val apkList = mutableListOf<ApkInfo>()
        
        try {
            val jsonArray = JSONArray(response)
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val name = item.getString("name")
                val type = item.getString("type")
                
                // 只处理APK文件
                if (type == "file" && name.endsWith(".apk")) {
                    val matchResult = APK_PATTERN.find(name)
                    if (matchResult != null) {
                        val version = matchResult.groupValues[1]
                        val timestamp = matchResult.groupValues[2]
                        val downloadUrl = GITHUB_RAW_URL + name
                        
                        apkList.add(ApkInfo(
                            fileName = name,
                            version = version,
                            timestamp = timestamp,
                            downloadUrl = downloadUrl
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("解析响应失败: ${e.message}")
        }
        
        return apkList
    }

    /**
     * 从APK列表中找到最新版本
     */
    private fun findLatestApk(apkList: List<ApkInfo>): ApkInfo {
        return apkList.maxWithOrNull(Comparator { a, b ->
            // 先比较版本号
            val versionCompare = compareVersions(a.version, b.version)
            if (versionCompare != 0) {
                versionCompare
            } else {
                // 版本号相同则比较时间戳
                a.timestamp.compareTo(b.timestamp)
            }
        }) ?: apkList.first()
    }
    
    /**
     * 比较两个版本号
     * 返回值：正数表示v1大于v2，负数表示v1小于v2，0表示相等
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = parseVersion(v1)
        val parts2 = parseVersion(v2)
        val maxLength = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1 - p2
            }
        }
        return 0
    }

    /**
     * 解析版本号为可比较的数值列表
     * 例如 "1.0.2" -> [1, 0, 2]
     */
    private fun parseVersion(version: String): List<Int> {
        return version.split(".").mapNotNull { it.toIntOrNull() }
    }

    /**
     * 比较版本号，判断是否有更新
     */
    private fun isNewerVersion(
        latestVersion: String,
        latestTimestamp: String,
        currentVersion: String,
        currentTimestamp: String
    ): Boolean {
        val latestParts = parseVersion(latestVersion)
        val currentParts = parseVersion(currentVersion)
        
        // 比较版本号
        val maxLength = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val latest = latestParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            
            when {
                latest > current -> return true
                latest < current -> return false
            }
        }
        
        // 版本号相同，比较时间戳
        if (currentTimestamp.isNotEmpty() && latestTimestamp.isNotEmpty()) {
            return latestTimestamp > currentTimestamp
        }
        
        // 无法判断时间戳，视为没有更新
        return false
    }

    /**
     * 格式化时间戳为可读格式
     * 输入: 20251225141707
     * 输出: 2025-12-25 14:17:07
     */
    fun formatTimestamp(timestamp: String): String {
        return try {
            if (timestamp.length == 14) {
                val year = timestamp.substring(0, 4)
                val month = timestamp.substring(4, 6)
                val day = timestamp.substring(6, 8)
                val hour = timestamp.substring(8, 10)
                val minute = timestamp.substring(10, 12)
                val second = timestamp.substring(12, 14)
                "$year-$month-$day $hour:$minute:$second"
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}