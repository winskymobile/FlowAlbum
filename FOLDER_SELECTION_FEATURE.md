# 文件夹选择和图片选择功能说明

## 功能概述

添加文件夹浏览和图片选择功能，让用户可以：
1. 浏览设备上的图片文件夹
2. 选择特定文件夹查看其中的图片
3. 单独选择某些图片或全选播放
4. 开始幻灯片播放选中的图片

## 需要创建的新文件

### 1. 数据模型类
- ✅ **PhotoFolder.kt** - 已创建
  - 存储文件夹信息（路径、名称、图片数量、封面图）

### 2. Activity类

#### BrowseActivity.kt
启动界面，用于浏览文件夹和选择图片：
- 显示所有包含图片的文件夹列表
- 点击文件夹进入图片选择界面
- 提供"所有图片"选项直接播放全部
- 权限请求和处理

#### PhotoSelectionActivity.kt
图片选择界面：
- 显示选中文件夹内的所有图片（网格布局）
- 支持单选/多选图片
- 全选/取消全选功能
- 确认选择后启动幻灯片播放

### 3. 适配器类

#### FolderAdapter.kt
文件夹列表适配器：
- 使用 RecyclerView 显示文件夹
- 显示文件夹名称、图片数量、封面图
- 支持遥控器导航和点击

#### PhotoGridAdapter.kt
图片网格适配器：
- 使用 RecyclerView GridLayoutManager
- 显示图片缩略图
- 支持选中/取消选中状态
- 遥控器导航优化

### 4. 布局文件

#### activity_browse.xml
浏览界面布局：
```xml
- RecyclerView（文件夹列表）
- 加载进度条
- 空状态提示
- 顶部标题栏
```

#### activity_photo_selection.xml
图片选择界面布局：
```xml
- RecyclerView（图片网格）
- 底部控制栏（全选、开始播放）
- 选中数量提示
- 返回按钮
```

#### item_folder.xml
文件夹列表项布局：
```xml
- 封面图片
- 文件夹名称
- 图片数量
- 选中状态指示
```

#### item_photo_grid.xml
图片网格项布局：
```xml
- 图片缩略图
- 选中状态（复选框或覆盖层）
- 焦点状态
```

### 5. 扩展 PhotoLoader.kt

添加新方法：
```kotlin
// 获取所有图片文件夹（带统计信息）
suspend fun getPhotoFoldersWithInfo(): List<PhotoFolder>

// 获取文件夹内的所有图片
suspend fun getPhotosInFolder(folderPath: String): List<Photo>
```

### 6. 更新 AndroidManifest.xml

添加新的 Activity：
```xml
<activity
    android:name=".BrowseActivity"
    android:exported="true"
    android:theme="@style/Theme.FlowAlbum">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".PhotoSelectionActivity"
    android:theme="@style/Theme.FlowAlbum" />

<activity
    android:name=".SlideshowActivity"
    android:theme="@style/Theme.FlowAlbum.Fullscreen" />
```

### 7. 修改 SlideshowActivity.kt

添加接收选中图片列表的功能：
```kotlin
companion object {
    const val EXTRA_PHOTO_URIS = "extra_photo_uris"
    const val EXTRA_FOLDER_PATH = "extra_folder_path"
}

// 从 Intent 获取图片列表
private fun loadPhotosFromIntent() {
    val photoUris = intent.getStringArrayListExtra(EXTRA_PHOTO_URIS)
    if (photoUris != null) {
        // 加载指定的图片
    } else {
        // 加载所有图片（原有逻辑）
    }
}
```

## 用户流程

### 流程 1：选择文件夹播放
1. 打开应用 → BrowseActivity
2. 显示所有图片文件夹列表
3. 用户选择一个文件夹
4. 进入 PhotoSelectionActivity 显示该文件夹的图片
5. 用户点击"全部播放"或选择部分图片
6. 启动 SlideshowActivity 播放选中的图片

### 流程 2：播放所有图片
1. 打开应用 → BrowseActivity
2. 用户选择"所有图片"选项
3. 直接启动 SlideshowActivity 播放全部图片

### 流程 3：选择特定图片
1. 打开应用 → BrowseActivity
2. 选择文件夹 → PhotoSelectionActivity
3. 单独勾选想要播放的图片
4. 点击"开始播放"
5. SlideshowActivity 仅播放选中的图片

## 遥控器操作设计

### BrowseActivity
- **方向键上/下**：在文件夹列表中移动
- **确认键**：进入选中的文件夹
- **返回键**：退出应用

### PhotoSelectionActivity
- **方向键**：在图片网格中移动焦点
- **确认键**：选中/取消选中当前图片
- **菜单键**：全选/取消全选
- **播放键**：开始播放选中的图片
- **返回键**：返回文件夹列表

## 数据传递

使用 Intent 在 Activity 之间传递数据：

```kotlin
// BrowseActivity → PhotoSelectionActivity
val intent = Intent(this, PhotoSelectionActivity::class.java)
intent.putExtra("folder_path", folder.path)
intent.putExtra("folder_name", folder.name)
startActivity(intent)

// PhotoSelectionActivity → SlideshowActivity
val intent = Intent(this, SlideshowActivity::class.java)
val selectedUris = selectedPhotos.map { it.uri.toString() }
intent.putStringArrayListExtra("photo_uris", ArrayList(selectedUris))
startActivity(intent)
```

## 实现优先级

如果时间有限，可以按以下优先级实现：

### 阶段1（核心功能）
1. ✅ PhotoFolder 数据模型
2. BrowseActivity（文件夹列表）
3. 扩展 PhotoLoader（获取文件夹信息）
4. 修改 SlideshowActivity（支持文件夹路径）

### 阶段2（选择功能）
5. PhotoSelectionActivity（图片网格）
6. PhotoGridAdapter（图片适配器）
7. 多选逻辑实现

### 阶段3（优化）
8. UI 美化和动画
9. 遥控器优化
10. 缓存和性能优化

## 技术要点

### RecyclerView 焦点处理
```kotlin
// 确保 RecyclerView 项可以获取焦点
itemView.isFocusable = true
itemView.isFocusableInTouchMode = true

// 监听焦点变化
itemView.setOnFocusChangeListener { view, hasFocus ->
    if (hasFocus) {
        // 高亮显示
        view.scaleX = 1.1f
        view.scaleY = 1.1f
    } else {
        // 恢复正常
        view.scaleX = 1.0f
        view.scaleY = 1.0f
    }
}
```

### 图片缩略图加载
```kotlin
// 使用 Glide 加载缩略图
Glide.with(context)
    .load(photo.uri)
    .thumbnail(0.1f)  // 10% 大小的缩略图
    .centerCrop()
    .into(imageView)
```

### 选中状态管理
```kotlin
class PhotoGridAdapter {
    private val selectedPhotos = mutableSetOf<Int>()  // 存储选中的位置
    
    fun toggleSelection(position: Int) {
        if (selectedPhotos.contains(position)) {
            selectedPhotos.remove(position)
        } else {
            selectedPhotos.add(position)
        }
        notifyItemChanged(position)
    }
    
    fun getSelectedPhotos(): List<Photo> {
        return selectedPhotos.map { photos[it] }
    }
}
```

## 预计工作量

- **数据模型和工具类扩展**：1-2小时
- **BrowseActivity 和布局**：2-3小时
- **PhotoSelectionActivity 和布局**：3-4小时
- **适配器实现**：2-3小时
- **集成和测试**：2-3小时
- **总计**：10-15小时

## 后续增强

功能稳定后可以考虑：
1. 添加搜索功能
2. 图片排序选项（按名称、日期、大小）
3. 文件夹收藏功能
4. 最近播放记录
5. 播放列表保存
6. 文件夹封面自定义

---

**注意**：这是一个较大的功能扩展，建议分阶段实现和测试，确保每个阶段都能正常工作后再继续下一阶段。