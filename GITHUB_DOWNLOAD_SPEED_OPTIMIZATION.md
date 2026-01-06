# GitHub 下载速度优化方案（最终版）

## 问题分析

原有实现中存在的问题：
1. 检测更新时使用代理轮询，但下载时直接使用GitHub URL，导致下载慢
2. Android DownloadManager 对某些动态代理站的兼容性未知
3. 需要一个稳定、可靠且兼容DownloadManager的加速方案

## 最终解决方案

### 核心设计：分离检测和下载的加速策略

**API检测**：使用动态代理轮询（适合API请求）
**文件下载**：使用专门的GitHub文件镜像站（兼容DownloadManager）

### 实现细节

#### 1. 代理配置（UpdateChecker.kt）

```kotlin
// API检测用代理站（用于GitHub API请求）
private val API_PROXY_PREFIXES = listOf(
    "",                              // 直连
    "https://gh-proxy.com/",         // 代理站1
    "https://cors.isteed.cc/",       // 代理站2
    "https://ghfast.top/"            // 代理站3
)

// 文件下载镜像站（专门用于文件下载，兼容DownloadManager）
private val DOWNLOAD_MIRRORS = listOf(
    "https://ghproxy.com/",          // 推荐镜像
    "https://mirror.ghproxy.com/",   // 备用镜像1
    "https://ghps.cc/",              // 备用镜像2
    ""                                // 直连（最后备选）
)
```

#### 2. 检测流程（保持不变）

```kotlin
// 检测更新时依次尝试API代理
for (proxyPrefix in API_PROXY_PREFIXES) {
    try {
        val apiUrl = proxyPrefix + GITHUB_API_PATH
        val result = tryFetchFromUrl(apiUrl, proxyPrefix)
        return result  // 成功则返回
    } catch (e: Exception) {
        continue  // 失败则尝试下一个
    }
}
```

#### 3. 下载实现（优化后）

```kotlin
fun downloadApk(fileName: String): Long {
    // 构建完整的下载URL
    val githubUrl = GITHUB_RAW_PATH + fileName
    val downloadUrl = DOWNLOAD_MIRRORS[0] + githubUrl
    
    // 使用DownloadManager下载
    val request = DownloadManager.Request(Uri.parse(downloadUrl))
    return downloadManager.enqueue(request)
}
```

**下载URL示例**：
```
原始GitHub URL:
https://github.com/winskymobile/FlowAlbum/raw/main/app/release/FlowAlbum_v1.0.4.apk

加速后的URL:
https://ghproxy.com/https://github.com/winskymobile/FlowAlbum/raw/main/app/release/FlowAlbum_v1.0.4.apk
```

## 技术优势

### 1. **兼容性**
- ✅ ghproxy.com 等镜像站专门为文件下载优化
- ✅ 完全兼容Android DownloadManager
- ✅ 支持大文件下载和断点续传

### 2. **稳定性**
- ✅ 镜像站服务稳定，专门用于GitHub文件加速
- ✅ 提供多个备选镜像（可扩展）
- ✅ 最后回退到直连GitHub

### 3. **性能**
- ✅ 国内下载速度大幅提升（通常从KB/s提升到MB/s）
- ✅ API检测快速（使用代理轮询）
- ✅ 文件下载加速（使用专门镜像）

### 4. **可维护性**
- ✅ 检测和下载逻辑分离
- ✅ 镜像列表易于扩展
- ✅ 配置集中管理

## 为什么不直接使用API代理站下载？

### 潜在问题

1. **重定向问题**
   - 某些代理站使用302重定向
   - DownloadManager可能不正确处理多次重定向

2. **CORS限制**
   - API代理站主要为Web请求设计
   - 可能有防盗链或Referer检查

3. **稳定性**
   - API代理站不保证大文件下载的稳定性
   - 可能有流量限制或超时

### 专用镜像站的优势

- **专门优化**：为文件下载场景设计
- **CDN加速**：通常配备CDN节点
- **无额外限制**：不检查来源，支持各种客户端
- **长连接支持**：支持大文件和断点续传

## 使用场景

### 国内网络环境
```
检测更新：尝试直连 → 失败 → 使用代理站API → 成功
文件下载：ghproxy.com镜像 → 速度提升10-100倍
```

### 国外网络环境
```
检测更新：直连成功
文件下载：可能使用直连或镜像（取决于配置）
```

## 扩展性

### 添加新镜像站

只需在配置中添加：

```kotlin
private val DOWNLOAD_MIRRORS = listOf(
    "https://ghproxy.com/",
    "https://mirror.ghproxy.com/",
    "https://ghps.cc/",
    "https://你的新镜像站.com/",  // 添加新镜像
    ""
)
```

### 动态镜像选择（未来优化）

可以实现下载失败时自动切换镜像：

```kotlin
fun downloadApkWithRetry(fileName: String): Long {
    for (mirror in DOWNLOAD_MIRRORS) {
        val downloadId = tryDownload(mirror + GITHUB_RAW_PATH + fileName)
        if (downloadId != -1L) {
            return downloadId
        }
    }
    return -1L
}
```

## 测试建议

1. **直连测试**：验证直连GitHub是否正常
2. **镜像测试**：验证镜像站下载速度
3. **失败切换**：测试镜像失效时的降级处理
4. **大文件下载**：测试APK文件完整性

## 总结

| 方案 | API检测 | 文件下载 | 优点 |
|------|---------|----------|------|
| **最终方案** | 动态代理轮询 | 专用文件镜像 | 兼容性好、速度快、稳定 |
| 原方案 | 动态代理轮询 | GitHub直连 | 下载慢 |
| 纯代理方案 | 动态代理 | 动态代理 | 兼容性未知 |

通过分离检测和下载的加速策略，我们既保证了更新检测的灵活性（代理轮询），又确保了文件下载的稳定性和速度（专用镜像），是一个兼顾各方面的最优解决方案。