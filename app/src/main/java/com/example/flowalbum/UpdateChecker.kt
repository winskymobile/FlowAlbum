package com.example.flowalbum

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
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
        // GitHub API原始地址（不含代理前缀）
        private const val GITHUB_API_PATH =
            "https://api.github.com/repos/winskymobile/FlowAlbum/contents/app/release"
        
        // GitHub Raw下载原始地址（不含代理前缀）
        private const val GITHUB_RAW_PATH =
            "https://github.com/winskymobile/FlowAlbum/raw/main/app/release/"
        
        // 代理站前缀列表（空字符串表示直接访问，其他为代理站前缀）
        // 按优先级排序：首先尝试直接访问，失败后依次尝试代理站
        private val PROXY_PREFIXES = listOf(
            "",                              // 直接访问（无代理）
            "https://ghproxy.com/",          // 代理站1（常用稳定）
            "https://gh-proxy.com/",         // 代理站2
            "https://mirror.ghproxy.com/",   // 代理站3（ghproxy镜像）
            "https://github.moeyy.xyz/",     // 代理站4
            "https://cors.isteed.cc/",       // 代理站5
            "https://ghfast.top/"            // 代理站6
        )
        
        // APK文件名正则表达式：FlowAlbum_v{版本号}_{时间戳}.apk
        private val APK_PATTERN = Regex("""FlowAlbum_v([\d.]+)_(\d{14})\.apk""")
        
        // 连接超时时间（毫秒）- 每个尝试的超时时间较短，以便快速切换到下一个代理
        private const val CONNECT_TIMEOUT = 8000
        private const val READ_TIMEOUT = 8000
    }
    
    // 当前成功使用的代理前缀（用于生成下载URL）
    private var successfulProxyPrefix: String = ""

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
                errorMessage = "检测更新失败，网络无响应请稍后重试"
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
     * 从BuildConfig.BUILD_TIMESTAMP获取
     */
    private fun getCurrentBuildTimestamp(): String {
        return try {
            // 从BuildConfig获取构建时间戳
            BuildConfig.BUILD_TIMESTAMP
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 从GitHub API获取APK文件列表（支持代理轮询）
     */
    private fun fetchApkListFromGitHub(): List<ApkInfo> {
        val errors = mutableListOf<String>()
        
        // 依次尝试直接访问和各个代理站
        for (proxyPrefix in PROXY_PREFIXES) {
            try {
                val apiUrl = proxyPrefix + GITHUB_API_PATH
                val result = tryFetchFromUrl(apiUrl, proxyPrefix)
                
                // 成功获取，记录当前使用的代理前缀
                successfulProxyPrefix = proxyPrefix
                return result
                
            } catch (e: Exception) {
                val source = if (proxyPrefix.isEmpty()) "直接访问" else "代理站 $proxyPrefix"
                errors.add("$source: ${e.message}")
                // 继续尝试下一个代理
                continue
            }
        }
        
        // 所有尝试都失败
        throw Exception("检测更新失败，网络无响应请稍后重试")
    }
    
    /**
     * 尝试从指定URL获取APK列表
     */
    private fun tryFetchFromUrl(apiUrl: String, proxyPrefix: String): List<ApkInfo> {
        val url = URL(apiUrl)
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
            return parseGitHubResponse(response, proxyPrefix)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 解析GitHub API响应
     */
    private fun parseGitHubResponse(response: String, proxyPrefix: String): List<ApkInfo> {
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
                        
                        // 优先使用 GitHub API 返回的 download_url（这是官方下载地址）
                        // 如果使用了代理站，则在 download_url 前添加代理前缀
                        val apiDownloadUrl = item.optString("download_url", "")
                        val downloadUrl = if (apiDownloadUrl.isNotEmpty()) {
                            // 使用 API 返回的下载地址，并添加代理前缀
                            proxyPrefix + apiDownloadUrl
                        } else {
                            // 备用方案：手动构建下载URL
                            proxyPrefix + GITHUB_RAW_PATH + name
                        }
                        
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
    
    /**
     * 从代理站 URL 中提取真实的 GitHub 下载地址
     * @param proxyUrl 代理站 URL
     * @return 真实的 GitHub 下载地址，如果不是代理站 URL 则返回原 URL
     */
    private fun extractRealUrl(proxyUrl: String): String {
        return try {
            // 检查是否是代理站 URL
            val proxyPrefixes = listOf(
                "https://ghproxy.com/",
                "https://gh-proxy.com/",
                "https://mirror.ghproxy.com/",
                "https://github.moeyy.xyz/",
                "https://cors.isteed.cc/",
                "https://ghfast.top/"
            )
            
            for (prefix in proxyPrefixes) {
                if (proxyUrl.startsWith(prefix)) {
                    // 移除代理站前缀，返回真实的 GitHub URL
                    return proxyUrl.substring(prefix.length)
                }
            }
            
            // 不是代理站 URL，直接返回原 URL
            proxyUrl
        } catch (e: Exception) {
            proxyUrl
        }
    }
    
    /**
     * 使用系统 DownloadManager 下载 APK
     * 自动处理代理站 URL，转换为真实的 GitHub 下载地址
     * @param downloadUrl 下载链接（可以是代理站链接或直接链接）
     * @param fileName APK 文件名
     * @return 下载任务 ID，如果失败返回 -1
     */
    fun downloadApk(downloadUrl: String, fileName: String): Long {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // 直接使用传入的下载地址（已包含代理前缀）
            // 不再提取真实URL，以便保持使用检测更新时成功的代理站
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                // 设置标题和描述
                setTitle("FlowAlbum 更新")
                setDescription("正在下载 $fileName")
                
                // 设置通知
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // 设置保存路径为公共下载目录
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                
                // 设置为可见和可管理
                setVisibleInDownloadsUi(true)
                
                // 允许使用所有网络类型下载（WiFi、移动数据、漫游）
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    setAllowedNetworkTypes(
                        DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE
                    )
                    // 允许在漫游时下载
                    @Suppress("DEPRECATION")
                    setAllowedOverRoaming(true)
                }
                
                // 允许在按流量计费的网络上下载（Android 11+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedOverMetered(true)
                }
                
                // 设置MIME类型
                setMimeType("application/vnd.android.package-archive")
                
                // 要求设备保持唤醒（防止下载中断）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setRequiresDeviceIdle(false)
                    setRequiresCharging(false)
                }
            }
            
            // 加入下载队列
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * 查询下载进度
     * @param downloadId 下载任务 ID
     * @return Pair<下载进度百分比(0-100), 下载状态描述>，失败返回 null
     */
    fun queryDownloadProgress(downloadId: Long): Pair<Int, String>? {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val downloadedBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                
                val status = cursor.getInt(statusIndex)
                val downloadedBytes = cursor.getLong(downloadedBytesIndex)
                val totalBytes = cursor.getLong(totalBytesIndex)
                val reason = cursor.getInt(reasonIndex)
                
                val progress = if (totalBytes > 0) {
                    ((downloadedBytes * 100) / totalBytes).toInt()
                } else {
                    0
                }
                
                val statusText = when (status) {
                    DownloadManager.STATUS_PENDING -> "准备下载..."
                    DownloadManager.STATUS_RUNNING -> "正在下载 $progress%"
                    DownloadManager.STATUS_PAUSED -> {
                        val pauseReason = when (reason) {
                            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "等待WiFi连接..."
                            DownloadManager.PAUSED_WAITING_TO_RETRY -> "网络不稳定，正在重试... $progress%"
                            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "网络连接中断，等待恢复..."
                            else -> "下载已暂停 $progress%"
                        }
                        pauseReason
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> "下载完成"
                    DownloadManager.STATUS_FAILED -> {
                        val failReason = when (reason) {
                            DownloadManager.ERROR_CANNOT_RESUME -> "下载失败：无法恢复下载"
                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "下载失败：未找到存储设备"
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "下载失败：文件已存在"
                            DownloadManager.ERROR_FILE_ERROR -> "下载失败：文件写入错误"
                            DownloadManager.ERROR_HTTP_DATA_ERROR -> "下载失败：网络数据错误"
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "下载失败：存储空间不足"
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "下载失败：链接重定向过多"
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "下载失败：服务器错误"
                            else -> "下载失败(错误代码:$reason)"
                        }
                        failReason
                    }
                    else -> "下载中..."
                }
                
                cursor.close()
                Pair(progress, statusText)
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 取消下载
     * @param downloadId 下载任务 ID
     */
    fun cancelDownload(downloadId: Long) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    /**
     * 获取下载文件的 Uri
     * @param downloadId 下载任务 ID
     * @return 下载文件的 Uri，失败返回 null
     */
    fun getDownloadedFileUri(downloadId: Long): Uri? {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.getUriForDownloadedFile(downloadId)
        } catch (e: Exception) {
            null
        }
    }
}