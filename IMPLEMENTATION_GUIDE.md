# 文件夹选择功能 - 快速实现指南

## 当前状态

✅ 已完成：
- PhotoFolder 数据模型
- dialog_folder_selection.xml 布局
- 字符串资源

⚠️ 需要实现：
- 文件夹选择对话框功能
- PhotoLoader 扩展方法
- SlideshowActivity 修改

## 最简实现方案（推荐）

由于完整实现需要较多代码，建议采用**简化版本**：在启动时显示文件夹选择对话框。

### 步骤 1：扩展 PhotoLoader

在 `PhotoLoader.kt` 中添加方法：

```kotlin
/**
 * 获取所有图片文件夹（带统计信息）
 */
suspend fun getPhotoFoldersWithInfo(): List<PhotoFolder> = withContext(Dispatchers.IO) {
    val foldersMap = mutableMapOf<String, MutableList<Photo>>()
    
    try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            
            while (cursor.moveToNext()) {
                try {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(pathColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    val photo = Photo(uri, path, name, size, dateAdded, dateModified)
                    val folder = File(path).parent ?: continue
                    
                    if (!foldersMap.containsKey(folder)) {
                        foldersMap[folder] = mutableListOf()
                    }
                    foldersMap[folder]?.add(photo)
                } catch (e: Exception) {
                    Log.e(TAG, "处理图片时出错: ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "获取文件夹失败: ${e.message}", e)
    }
    
    // 转换为 PhotoFolder 列表
    return@withContext foldersMap.map { (path, photos) ->
        PhotoFolder(
            path = path,
            name = File(path).name,
            photoCount = photos.size,
            coverPhoto = photos.firstOrNull()
        )
    }.sortedByDescending { it.photoCount }
}
```

### 步骤 2：修改 SlideshowActivity

在 `SlideshowActivity.kt` 的 `onCreate` 方法中，权限获取成功后显示文件夹选择对话框：

```kotlin
// 在 checkAndRequestPermissions() 成功后调用
private fun showFolderSelectionDialog() {
    lifecycleScope.launch {
        try {
            // 加载文件夹列表
            val folders = photoLoader.getPhotoFoldersWithInfo()
            
            // 显示对话框
            val dialogView = layoutInflater.inflate(R.layout.dialog_folder_selection, null)
            val dialog = AlertDialog.Builder(this@SlideshowActivity)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            val btnAllPhotos = dialogView.findViewById<Button>(R.id.btnAllPhotos)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewFolders)
            
            // "所有图片"按钮
            btnAllPhotos.setOnClickListener {
                dialog.dismiss()
                loadPhotos()  // 加载所有图片（原有逻辑）
            }
            
            // "取消"按钮
            btnCancel.setOnClickListener {
                dialog.dismiss()
                finish()
            }
            
            // 设置文件夹列表
            recyclerView.layoutManager = LinearLayoutManager(this@SlideshowActivity)
            val adapter = FolderAdapter(folders) { folder ->
                dialog.dismiss()
                loadPhotosFromFolder(folder.path)
            }
            recyclerView.adapter = adapter
            
            dialog.show()
        } catch (e: Exception) {
            Log.e("SlideshowActivity", "显示文件夹对话框失败", e)
            loadPhotos()  // 失败时直接加载所有图片
        }
    }
}

// 新增：从指定文件夹加载图片
private fun loadPhotosFromFolder(folderPath: String) {
    showLoading(true)
    
    lifecycleScope.launch {
        try {
            val photos = photoLoader.loadPhotosFromFolder(folderPath)
            photoList.clear()
            photoList.addAll(photos)
            showLoading(false)
            
            if (photoList.isEmpty()) {
                showNoPhotosMessage()
            } else {
                currentIndex = 0
                displayCurrentPhoto()
                updateIndicator()
                updatePhotoInfo()
                if (settingsManager.isAutoPlay()) {
                    startSlideshow()
                }
            }
        } catch (e: Exception) {
            showLoading(false)
            showError("加载文件夹图片失败: ${e.message}")
        }
    }
}
```

### 步骤 3：创建简单的 FolderAdapter

创建新文件 `FolderAdapter.kt`：

```kotlin
package com.example.flowalbum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private val folders: List<PhotoFolder>,
    private val onFolderClick: (PhotoFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(android.R.id.text1)
        val textCount: TextView = view.findViewById(android.R.id.text2)
        
        init {
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
            itemView.setOnClickListener {
                onFolderClick(folders[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.textName.text = folder.name
        holder.textCount.text = "${folder.photoCount} 张图片"
    }

    override fun getItemCount() = folders.size
}
```

### 步骤 4：修改 checkAndRequestPermissions

在权限授予后调用文件夹选择对话框：

```kotlin
private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // 显示文件夹选择对话框
        showFolderSelectionDialog()
    } else {
        showPermissionDeniedMessage()
    }
}
```

## 更简单的替代方案

如果上述方案仍然太复杂，可以采用**最简方案**：

### 在设置对话框中添加文件夹选择

1. 在 `dialog_settings.xml` 中添加一个"选择文件夹"按钮
2. 点击后弹出文件夹列表对话框
3. 选择文件夹后重新加载图片

这样用户可以在播放过程中切换文件夹。

## 测试步骤

1. 构建并安装应用
2. 启动应用，应该看到文件夹选择对话框
3. 选择"所有图片"应该播放全部图片
4. 选择特定文件夹应该只播放该文件夹的图片

## 故障排查

### 问题1：对话框不显示
- 检查权限是否正确授予
- 查看 Logcat 中的错误信息

### 问题2：文件夹列表为空
- 确认设备上有图片文件
- 检查 MediaStore 查询是否成功

### 问题3：选择文件夹后无图片
- 检查 `loadPhotosFromFolder` 方法的路径参数
- 确认 PhotoLoader 的过滤条件

## 后续增强

功能稳定后可以添加：
1. 图片预览（网格视图）
2. 多选功能
3. 文件夹搜索
4. 最近使用的文件夹

---

**重要提示**：由于这是一个较大的功能添加，建议先实现基本的文件夹选择，测试稳定后再考虑添加图片多选等高级功能。