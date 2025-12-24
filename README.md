# FlowAlbum - Android TV 相册应用

一个功能完整的 Android TV 相册应用，支持自动播放、多种动画效果和遥控器控制。

## 功能特性

### 核心功能
- ✅ **自动播放**：支持设置图片切换间隔（1-30秒）
- ✅ **多种动画效果**：淡入淡出、滑动（上下左右）、缩放、旋转
- ✅ **随机动画**：每次切换随机选择动画效果
- ✅ **全屏显示**：沉浸式全屏体验
- ✅ **图片指示器**：显示当前位置和总数
- ✅ **遥控器支持**：完整的TV遥控器操作支持

### 控制方式

#### 遥控器按键
- **方向键左/右**：切换上一张/下一张图片
- **方向键上/下**：显示/隐藏控制栏
- **确认键（OK）**：播放/暂停
- **菜单键**：打开设置对话框
- **返回键**：退出应用

#### 屏幕控制
- **点击图片**：显示/隐藏控制栏
- **控制按钮**：上一张、播放/暂停、下一张、设置

## 项目结构

```
app/src/main/
├── java/com/example/flowalbum/
│   ├── SlideshowActivity.kt        # 主Activity（幻灯片播放）
│   ├── Photo.kt                    # 图片数据模型
│   ├── PhotoLoader.kt              # 图片加载工具类
│   ├── AnimationHelper.kt          # 动画工具类
│   ├── SettingsManager.kt          # 设置管理类
│   └── IndicatorView.kt            # 自定义指示器视图
│
├── res/
│   ├── layout/
│   │   ├── activity_slideshow.xml  # 主界面布局
│   │   └── dialog_settings.xml     # 设置对话框布局
│   ├── values/
│   │   ├── strings.xml             # 字符串资源
│   │   ├── colors.xml              # 颜色资源
│   │   └── themes.xml              # 主题样式
│   └── ...
│
└── AndroidManifest.xml             # 应用清单文件
```

## 安装和运行

### 环境要求
- Android Studio Arctic Fox 或更高版本
- Android SDK API Level 21 或以上
- 支持 Kotlin 2.0.21

### 安装步骤

1. **克隆或下载项目**
   ```bash
   git clone <repository-url>
   cd FlowAlbum
   ```

2. **在 Android Studio 中打开项目**
   - 打开 Android Studio
   - 选择 "Open an Existing Project"
   - 选择项目根目录

3. **同步 Gradle**
   - 等待 Gradle 自动同步
   - 如果遇到问题，点击 "Sync Project with Gradle Files"

4. **连接 Android TV 设备或模拟器**
   - 真机：通过 ADB 连接您的 Android TV 设备
   - 模拟器：创建一个 Android TV 虚拟设备

5. **运行应用**
   - 点击 "Run" 按钮或按 `Shift + F10`
   - 选择目标设备
   - 等待应用安装并启动

### 构建 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

生成的 APK 文件位于：`app/build/outputs/apk/`

## 使用说明

### 首次运行

1. **授予权限**
   - 首次启动时，应用会请求存储权限
   - 点击"授予权限"以允许应用访问图片
   - Android 13+ 会请求 `READ_MEDIA_IMAGES` 权限

2. **加载图片**
   - 应用会自动扫描设备上的所有图片
   - 如果未找到图片，会显示提示信息

3. **开始播放**
   - 如果启用了自动播放，图片会自动开始轮播
   - 默认切换间隔为 3 秒

### 设置选项

按遥控器的**菜单键**或点击**设置按钮**打开设置对话框：

#### 1. 切换间隔
- 使用 `+` / `-` 按钮调整间隔（1-30秒）
- 实时显示当前设置的秒数

#### 2. 动画效果
- **动画类型**：从下拉列表选择具体动画
  - 淡入淡出
  - 左滑/右滑/上滑/下滑
  - 放大/缩小
  - 旋转
  - 随机效果
- **随机动画**：开启后每次切换使用随机动画

#### 3. 显示设置
- **显示指示器**：开启/关闭底部圆点指示器
- **自动播放**：启动时是否自动开始播放

#### 4. 操作按钮
- **重置**：恢复所有设置为默认值
- **应用**：保存设置并关闭对话框

## 自定义配置

### 修改图片来源

如果要指定特定文件夹的图片，可以修改 `SettingsManager.kt`：

```kotlin
// 在 SlideshowActivity.kt 的 loadPhotos() 方法中
// 将 null 替换为具体路径
val photos = photoLoader.loadPhotosFromDirectory("/sdcard/Pictures/MyPhotos")
```

### 修改默认设置

编辑 `SettingsManager.kt` 中的常量：

```kotlin
companion object {
    const val DEFAULT_INTERVAL = 3000L  // 默认间隔（毫秒）
    const val MIN_INTERVAL = 1000L      // 最小间隔
    const val MAX_INTERVAL = 30000L     // 最大间隔
}
```

### 添加新的动画效果

在 `AnimationHelper.kt` 中添加新的动画类型：

```kotlin
enum class AnimationType {
    // 现有类型...
    YOUR_NEW_ANIMATION  // 添加新类型
}

// 然后实现对应的动画方法
private fun applyYourNewAnimation(...) {
    // 动画实现
}
```

## 依赖库

项目使用以下主要依赖库：

```kotlin
// 核心库
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")

// TV Leanback 库
implementation("androidx.leanback:leanback:1.0.0")

// Activity 库
implementation("androidx.activity:activity-ktx:1.8.2")

// 图片加载库 Glide
implementation("com.github.bumptech.glide:glide:4.16.0")

// ViewPager2
implementation("androidx.viewpager2:viewpager2:1.0.0")

// ConstraintLayout
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// Material Design
implementation("com.google.android.material:material:1.11.0")

// 协程支持
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
```

## 代码说明

### 核心类详解

#### 1. SlideshowActivity
主Activity，负责：
- 权限管理和请求
- 图片加载和显示
- 自动播放控制
- 用户交互处理
- 设置对话框管理

关键方法：
```kotlin
loadPhotos()                    // 加载图片列表
displayPhotoWithAnimation()     // 带动画显示图片
startSlideshow()                // 开始自动播放
stopSlideshow()                 // 停止播放
showSettingsDialog()            // 显示设置对话框
```

#### 2. PhotoLoader
图片加载工具类，提供：
- 从 MediaStore 加载图片
- 从指定目录加载图片
- 获取图片文件夹列表

#### 3. AnimationHelper
动画管理类，提供：
- 8种预定义动画效果
- 随机动画选择
- 动画参数配置

#### 4. SettingsManager
设置管理类，使用 SharedPreferences 保存：
- 切换间隔
- 动画类型
- 显示选项
- 自动播放状态

#### 5. IndicatorView
自定义视图，显示：
- 当前图片位置
- 图片总数
- 圆点指示器（支持省略显示）

## 常见问题

### 1. 应用启动后看不到图片
**解决方案**：
- 确保已授予存储权限
- 检查设备上是否有图片文件
- 在设置中查看图片文件夹路径

### 2. 动画效果不流畅
**解决方案**：
- 确保设备性能足够
- 减小图片文件大小
- 增加切换间隔时间

### 3. 遥控器按键无响应
**解决方案**：
- 确保在 TV 设备上运行
- 检查 AndroidManifest.xml 中的 TV 配置
- 重启应用尝试

### 4. 权限被拒绝
**解决方案**：
- Android 13+：需要 READ_MEDIA_IMAGES 权限
- Android 12 及以下：需要 READ_EXTERNAL_STORAGE 权限
- 在系统设置中手动授予权限

## 技术特点

1. **现代化架构**
   - 使用 Kotlin 协程处理异步操作
   - MVVM 模式分离逻辑
   - 组件化设计便于维护

2. **性能优化**
   - Glide 图片加载和缓存
   - 视图复用减少内存占用
   - 异步加载避免UI阻塞

3. **用户体验**
   - 流畅的动画过渡
   - 直观的遥控器操作
   - 完整的错误处理

4. **可扩展性**
   - 模块化设计
   - 易于添加新功能
   - 支持自定义配置

## 开发者信息

- **项目名称**：FlowAlbum
- **版本**：1.0
- **最低 SDK**：21 (Android 5.0)
- **目标 SDK**：34 (Android 14)

## 许可证

本项目仅供学习和参考使用。

## 更新日志

### v1.0 (2024-12-19)
- ✅ 初始版本发布
- ✅ 实现基础幻灯片功能
- ✅ 添加多种动画效果
- ✅ 完整的遥控器支持
- ✅ 设置管理功能
- ✅ 自定义指示器视图

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至开发者

---

**享受您的 Android TV 相册体验！** 📺📸