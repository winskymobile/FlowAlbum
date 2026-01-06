# GitHub下载加速方案对比

## 问题分析

Android DownloadManager 使用代理站URL可能存在以下问题：
1. 代理站可能有防盗链或Referer检查
2. 重定向机制可能不被DownloadManager正确处理
3. 代理站稳定性不如官方源

## 推荐方案

### 方案一：使用GitHub镜像加速域名（推荐）★★★★★

使用稳定的GitHub加速域名替换官方域名：

```kotlin
private const val GITHUB_MIRROR = "https://download.fastgit.org"
// 或其他稳定镜像：
// "https://hub.fastgit.xyz"
// "https://ghproxy.com"

// 将 raw.githubusercontent.com 替换为镜像域名
// 原始: https://github.com/user/repo/raw/main/file.apk
// 加速: https://download.fastgit.org/user/repo/raw/main/file.apk
```

**优点**：
- ✅ DownloadManager完全兼容
- ✅ 下载稳定可靠
- ✅ 速度提升明显
- ✅ 实现简单

**缺点**：
- ⚠️ 需要维护镜像域名列表
- ⚠️ 镜像站可能失效需要更新

### 方案二：自定义下载器 + 代理 ★★★★☆

使用OkHttp等网络库实现自定义下载，支持代理：

```kotlin
// 使用OkHttp自定义下载
private suspend fun downloadWithProxy(url: String, file: File) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    val request = Request.Builder()
        .url(url)
        .build()
        
    client.newCall(request).execute().use { response ->
        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
```

**优点**：
- ✅ 完全控制下载过程
- ✅ 支持进度回调
- ✅ 可使用代理站
- ✅ 支持断点续传

**缺点**：
- ⚠️ 实现复杂度高
- ⚠️ 需要处理生命周期
- ⚠️ 需要添加OkHttp依赖

### 方案三：保持直连 + 优化重试 ★★★☆☆

优化当前实现，增加智能重试和多源备份：

```kotlin
// 准备多个下载源
private val DOWNLOAD_SOURCES = listOf(
    "https://github.com/user/repo/releases/download/",
    "https://download.fastgit.org/user/repo/releases/download/",
    "https://hub.fastgit.xyz/user/repo/releases/download/"
)

// 下载失败时自动尝试下一个源
```

**优点**：
- ✅ 实现简单
- ✅ 兼容性好
- ✅ 有备选方案

**缺点**：
- ⚠️ 首次尝试仍然慢
- ⚠️ 需要用户手动重试

## 最佳实践建议

### 推荐实现（方案一的改进版）

```kotlin
companion object {
    // GitHub官方raw地址
    private const val GITHUB_RAW = 
        "https://github.com/winskymobile/FlowAlbum/raw/main/app/release/"
    
    // GitHub加速镜像列表（用于下载）
    private val DOWNLOAD_MIRRORS = listOf(
        "https://ghproxy.com/https://github.com/winskymobile/FlowAlbum/raw/main/app/release/",
        "https://mirror.ghproxy.com/https://github.com/winskymobile/FlowAlbum/raw/main/app/release/",
        GITHUB_RAW  // 官方源作为最后备选
    )
    
    // API检测使用代理轮询（保持不变）
    private val API_PROXY_PREFIXES = listOf(
        "",
        "https://gh-proxy.com/",
        "https://cors.isteed.cc/",
        "https://ghfast.top/"
    )
}

// 下载时使用专门的镜像地址
fun downloadApk(fileName: String): Long {
    // 尝试第一个镜像地址
    val downloadUrl = DOWNLOAD_MIRRORS[0] + fileName
    
    val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
        // ... 其他配置
    }
    
    return downloadManager.enqueue(request)
}
```

### 关键要点

1. **API检测**：使用代理轮询（已实现）
2. **实际下载**：使用专门的下载镜像（兼容DownloadManager）
3. **失败重试**：下载失败时提示用户重试，自动切换到下一个镜像
4. **分离逻辑**：检测和下载使用不同的加速策略

## 验证方法

建议先进行小规模测试：

```kotlin
// 测试代码
fun testDownloadUrl(url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    // 下载测试文件验证可行性
}
```

## 结论

- **当前方案有风险**：DownloadManager直接使用代理站URL可能不稳定
- **推荐改用镜像域名**：专门的GitHub镜像服务更稳定可靠
- **分离检测和下载**：API检测可用代理，实际下载用镜像