package com.example.flowalbum

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Method

/**
 * 图片加载工具类
 * 用于从设备存储中加载图片列表
 */
class PhotoLoader(private val context: Context) {

    companion object {
        private const val TAG = "PhotoLoader"
        
        // 支持的图片格式
        private val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    /**
     * 使用StorageManager API检测所有外置存储卷
     * 这是系统级别的自动识别方法，不依赖硬编码路径
     */
    private fun detectStorageVolumes(): List<String> {
        val paths = mutableListOf<String>()
        
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (storageManager == null) {
                Log.e(TAG, "无法获取StorageManager服务")
                return paths
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用公开API
                try {
                    val storageVolumes = storageManager.storageVolumes
                    storageVolumes.forEach { volume ->
                        try {
                            // 使用反射获取路径（官方API没有直接提供）
                            val getPathMethod = volume.javaClass.getMethod("getPath")
                            val path = getPathMethod.invoke(volume) as? String
                            
                            // 检查是否可移除
                            val isRemovable = volume.isRemovable
                            // 检查状态
                            val state = volume.state
                            
                            Log.d(TAG, "存储卷[API24+]: path=$path, state=$state, removable=$isRemovable")
                            
                            // 如果是已挂载的可移除存储（U盘、SD卡等）
                            if (path != null && state == Environment.MEDIA_MOUNTED && isRemovable) {
                                paths.add(path)
                                Log.d(TAG, "✓ 添加可移除存储: $path")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "处理存储卷失败: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "使用StorageVolume API失败，尝试反射方法: ${e.message}")
                    // 如果新API失败，回退到反射方法
                    paths.addAll(detectStorageVolumesReflection(storageManager))
                }
            } else {
                // Android 7.0以下使用反射
                paths.addAll(detectStorageVolumesReflection(storageManager))
            }
        } catch (e: Exception) {
            Log.e(TAG, "检测存储卷失败: ${e.message}", e)
        }
        
        Log.d(TAG, "系统级检测完成，找到 ${paths.size} 个外置存储设备")
        return paths
    }
    
    /**
     * 使用反射方法检测存储卷（兼容Android 7.0以下版本）
     */
    private fun detectStorageVolumesReflection(storageManager: StorageManager): List<String> {
        val paths = mutableListOf<String>()
        
        try {
            val getVolumeList: Method = storageManager.javaClass.getMethod("getVolumeList")
            val volumeList = getVolumeList.invoke(storageManager) as? Array<*>
            
            volumeList?.forEach { volume ->
                if (volume == null) return@forEach
                
                try {
                    // 获取路径
                    val getPath: Method = volume.javaClass.getMethod("getPath")
                    val path = getPath.invoke(volume) as? String
                    
                    // 获取状态
                    val getState: Method = volume.javaClass.getMethod("getState")
                    val state = getState.invoke(volume) as? String
                    
                    // 检查是否可移除
                    val isRemovable: Method = volume.javaClass.getMethod("isRemovable")
                    val removable = isRemovable.invoke(volume) as? Boolean
                    
                    Log.d(TAG, "存储卷[反射]: path=$path, state=$state, removable=$removable")
                    
                    // 如果是已挂载的可移除存储
                    if (path != null && state == "mounted" && removable == true) {
                        paths.add(path)
                        Log.d(TAG, "✓ 添加可移除存储: $path")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理存储卷失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "反射获取存储卷列表失败: ${e.message}")
        }
        
        return paths
    }
    
    /**
     * 检测外置存储设备路径（U盘、SD卡等）
     * 完全使用系统级别API自动识别，不依赖硬编码路径
     * @return 外置存储设备路径列表
     */
    private fun detectUsbPaths(): List<String> {
        val externalPaths = mutableSetOf<String>()
        
        Log.d(TAG, "========== 开始系统级检测外置存储设备 ==========")
        
        // 方法1: 使用StorageManager API（最可靠）
        val storagePaths = detectStorageVolumes()
        externalPaths.addAll(storagePaths)
        Log.d(TAG, "✓ StorageManager API检测到 ${storagePaths.size} 个外置存储设备")
        storagePaths.forEach { path ->
            Log.d(TAG, "  - $path")
        }
        
        // 方法2: 通过Environment获取外置存储目录（Android 4.4+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val externalDirs = context.getExternalFilesDirs(null)
                externalDirs.forEach { dir ->
                    if (dir != null) {
                        val isRemovable = Environment.isExternalStorageRemovable(dir)
                        val state = Environment.getExternalStorageState(dir)
                        
                        Log.d(TAG, "外部存储: ${dir.absolutePath}, removable=$isRemovable, state=$state")
                        
                        if (isRemovable && state == Environment.MEDIA_MOUNTED) {
                            // 提取到存储根目录
                            val storagePath = extractStorageRootPath(dir.absolutePath)
                            if (storagePath != null) {
                                externalPaths.add(storagePath)
                                Log.d(TAG, "✓ 通过Environment添加外置存储根路径: $storagePath")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "通过Environment检测失败: ${e.message}")
            }
        }
        
        // 方法3: 扫描/storage目录（补充方法，识别StorageManager可能遗漏的设备）
        try {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.canRead()) {
                storageDir.listFiles()?.forEach { dir ->
                    val name = dir.name.lowercase()
                    
                    // 排除系统内置存储
                    val isSystemStorage = name == "emulated" ||
                                        name == "self" ||
                                        name == "sdcard"
                    
                    if (!isSystemStorage && dir.isDirectory && dir.canRead()) {
                        // 尝试读取以验证是否真的可访问
                        try {
                            val files = dir.listFiles()
                            if (files != null) {
                                externalPaths.add(dir.absolutePath)
                                Log.d(TAG, "✓ /storage扫描添加: ${dir.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "  /storage/${dir.name} 不可访问")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描/storage目录失败: ${e.message}")
        }
        
        val distinctPaths = externalPaths.toList()
        Log.d(TAG, "========== 系统级检测完成: 找到 ${distinctPaths.size} 个外置存储设备 ==========")
        distinctPaths.forEach { path ->
            Log.d(TAG, "  ✓ $path")
        }
        
        return distinctPaths
    }
    
    /**
     * 从应用私有目录路径提取存储根路径
     * 例如: /storage/1234-5678/Android/data/com.example.app/files -> /storage/1234-5678
     */
    private fun extractStorageRootPath(path: String): String? {
        try {
            // 查找 /storage/ 后的第一个目录
            val storageIndex = path.indexOf("/storage/")
            if (storageIndex != -1) {
                val afterStorage = path.substring(storageIndex + 9) // "/storage/".length = 9
                val nextSlash = afterStorage.indexOf("/")
                if (nextSlash != -1) {
                    return "/storage/" + afterStorage.substring(0, nextSlash)
                } else {
                    return "/storage/$afterStorage"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "提取存储根路径失败: $path, ${e.message}")
        }
        return null
    }
    
    /**
     * 判断路径是否为外置存储设备路径
     * 使用系统检测到的路径和通用模式匹配
     */
    private fun isUsbPath(path: String): Boolean {
        // 检查是否在系统检测到的外置存储路径中
        val detectedPaths = detectUsbPaths()
        if (detectedPaths.any { path.startsWith(it) }) {
            return true
        }
        
        // 补充模式匹配（适用于某些特殊情况）
        val pathLower = path.lowercase()
        
        // 排除内置存储
        if (pathLower.contains("/emulated/") ||
            pathLower.contains("/sdcard/") ||
            path.startsWith("/data/")) {
            return false
        }
        
        // 匹配常见的外置存储模式
        return pathLower.contains("/usb") ||
               pathLower.contains("/udisk") ||
               pathLower.contains("/otg") ||
               path.matches(Regex(".*/[0-9A-F]{4}-[0-9A-F]{4}/.*")) || // FAT32 UUID格式
               path.matches(Regex(".*/[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}/.*")) // 扩展UUID格式
    }

    /**
     * 从指定文件夹加载所有图片
     * @param folderPath 文件夹路径，如果为空则加载所有图片
     * @return 图片列表
     */
    suspend fun loadPhotosFromFolder(folderPath: String? = null): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        
        // 如果是外置设备路径，直接使用File API
        if (folderPath != null && (folderPath.startsWith("/mnt/") || folderPath.startsWith("/storage/"))) {
            Log.d(TAG, "使用File API加载外置设备图片: $folderPath")
            return@withContext loadPhotosFromDirectory(folderPath)
        }
        
        try {
            // 定义查询的列
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            
            // 构建查询条件
            val selection = if (folderPath != null) {
                "${MediaStore.Images.Media.DATA} LIKE ?"
            } else {
                null
            }
            
            val selectionArgs = if (folderPath != null) {
                arrayOf("$folderPath%")
            } else {
                null
            }
            
            // 按修改时间降序排列
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            
            // 查询图片
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
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
                        
                        // 构建URI
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        
                        // 创建Photo对象并添加到列表
                        photos.add(
                            Photo(
                                uri = uri,
                                path = path,
                                name = name,
                                size = size,
                                dateAdded = dateAdded,
                                dateModified = dateModified
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "加载图片时出错: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "成功加载 ${photos.size} 张图片")
        } catch (e: Exception) {
            Log.e(TAG, "加载图片列表失败: ${e.message}", e)
        }
        
        return@withContext photos
    }

    /**
     * 从指定目录加载图片（使用File API）
     * 适用于应用私有目录或已授权的特定目录
     * @param directory 目录路径
     * @return 图片列表
     */
    suspend fun loadPhotosFromDirectory(directory: String): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        
        try {
            val dir = File(directory)
            
            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "目录不存在或不是有效目录: $directory")
                return@withContext photos
            }
            
            // 递归加载所有图片
            loadPhotosRecursively(dir, photos)
            
            // 按修改时间排序
            photos.sortByDescending { it.dateModified }
            
            Log.d(TAG, "从目录 $directory 递归加载了 ${photos.size} 张图片")
        } catch (e: Exception) {
            Log.e(TAG, "从目录加载图片失败: ${e.message}", e)
        }
        
        return@withContext photos
    }
    
    /**
     * 递归加载目录下的所有图片（跳过隐藏文件）
     */
    private fun loadPhotosRecursively(dir: File, photos: MutableList<Photo>) {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return
        
        // 跳过隐藏目录
        if (dir.name.startsWith(".")) {
            Log.d(TAG, "跳过隐藏目录: ${dir.name}")
            return
        }
        
        try {
            // 获取当前目录中的所有图片文件（排除隐藏文件）
            dir.listFiles { file ->
                file.isFile &&
                !file.name.startsWith(".") &&
                SUPPORTED_EXTENSIONS.any {
                    file.extension.equals(it, ignoreCase = true)
                }
            }?.forEach { file ->
                try {
                    val uri = Uri.fromFile(file)
                    photos.add(
                        Photo(
                            uri = uri,
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            dateAdded = file.lastModified() / 1000,
                            dateModified = file.lastModified() / 1000
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "处理文件时出错: ${file.name}, ${e.message}")
                }
            }
            
            // 递归处理子目录（排除隐藏目录）
            dir.listFiles { file ->
                file.isDirectory && !file.name.startsWith(".")
            }?.forEach { subDir ->
                loadPhotosRecursively(subDir, photos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "递归加载图片失败: ${dir.absolutePath}, ${e.message}")
        }
    }

    /**
     * 获取所有包含图片的文件夹
     * @return 文件夹路径列表
     */
    suspend fun getPhotoFolders(): List<String> = withContext(Dispatchers.IO) {
        val folders = mutableSetOf<String>()
        
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                
                while (cursor.moveToNext()) {
                    try {
                        val path = cursor.getString(pathColumn)
                        val folder = File(path).parent
                        if (folder != null) {
                            folders.add(folder)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "获取文件夹时出错: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "找到 ${folders.size} 个图片文件夹")
        } catch (e: Exception) {
            Log.e(TAG, "获取图片文件夹失败: ${e.message}", e)
        }
        
        return@withContext folders.sorted()
    }

    /**
     * 扫描U盘中的图片文件夹
     * @return 包含图片的U盘文件夹列表
     */
    suspend fun scanUsbFolders(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val usbFolders = mutableListOf<PhotoFolder>()
        val usbPaths = detectUsbPaths()
        
        usbPaths.forEach { usbPath ->
            try {
                val dir = File(usbPath)
                // 递归扫描U盘中的文件夹
                scanDirectoryForPhotos(dir, usbFolders)
            } catch (e: Exception) {
                Log.e(TAG, "扫描USB路径失败: $usbPath, ${e.message}")
            }
        }
        
        Log.d(TAG, "在U盘中找到 ${usbFolders.size} 个包含图片的文件夹")
        return@withContext usbFolders.sortedByDescending { it.photoCount }
    }
    
    /**
     * 递归扫描目录查找包含图片的文件夹（跳过隐藏文件）
     */
    private fun scanDirectoryForPhotos(dir: File, folderList: MutableList<PhotoFolder>) {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return
        
        // 跳过隐藏目录
        if (dir.name.startsWith(".")) {
            return
        }
        
        try {
            val photoFiles = dir.listFiles { file ->
                file.isFile &&
                !file.name.startsWith(".") &&
                SUPPORTED_EXTENSIONS.any {
                    file.extension.equals(it, ignoreCase = true)
                }
            }
            
            // 如果当前目录包含图片，添加到列表
            if (photoFiles != null && photoFiles.isNotEmpty()) {
                val photos = photoFiles.map { file ->
                    Photo(
                        uri = Uri.fromFile(file),
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        dateModified = file.lastModified() / 1000
                    )
                }.sortedByDescending { it.dateModified }
                
                folderList.add(
                    PhotoFolder(
                        path = dir.absolutePath,
                        name = dir.name,
                        photoCount = photos.size,
                        coverPhoto = photos.firstOrNull()
                    )
                )
            }
            
            // 递归扫描子目录（排除隐藏目录，限制深度避免过深）
            val subDirs = dir.listFiles { file ->
                file.isDirectory && !file.name.startsWith(".")
            }
            subDirs?.take(50)?.forEach { subDir ->
                scanDirectoryForPhotos(subDir, folderList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描目录失败: ${dir.absolutePath}, ${e.message}")
        }
    }
    
    /**
     * 通过MediaStore查找外置设备中的图片
     */
    private suspend fun findExternalDeviceFoldersFromMediaStore(): Map<String, MutableList<Photo>> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, MutableList<Photo>>()
        
        try {
            Log.d(TAG, "开始从MediaStore查找外置设备图片...")
            
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
                null
            )?.use { cursor ->
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                
                var totalImages = 0
                var usbImages = 0
                
                while (cursor.moveToNext()) {
                    try {
                        totalImages++
                        val path = cursor.getString(pathColumn)
                        
                        // 检查是否是U盘路径
                        if (isUsbPath(path)) {
                            usbImages++
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val size = cursor.getLong(sizeColumn)
                            val dateAdded = cursor.getLong(dateAddedColumn)
                            val dateModified = cursor.getLong(dateModifiedColumn)
                            
                            // 获取根目录下的一级目录
                            val file = File(path)
                            var parentDir = file.parentFile
                            
                            // 向上查找，直到找到U盘根目录的直接子目录
                            while (parentDir != null && parentDir.parent != null) {
                                val parentPath = parentDir.parent
                                if (parentPath != null && isUsbPath(parentPath)) {
                                    // 父目录的父目录是U盘根目录，当前parentDir就是一级目录
                                    break
                                }
                                parentDir = parentDir.parentFile
                            }
                            
                            if (parentDir != null) {
                                val topPath = parentDir.absolutePath
                                
                                val uri = ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                                
                                val photo = Photo(
                                    uri = uri,
                                    path = path,
                                    name = name,
                                    size = size,
                                    dateAdded = dateAdded,
                                    dateModified = dateModified
                                )
                                
                                if (!folderMap.containsKey(topPath)) {
                                    folderMap[topPath] = mutableListOf()
                                    Log.d(TAG, "发现外置设备目录: $topPath (来自图片: $path)")
                                }
                                folderMap[topPath]?.add(photo)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理图片记录失败: ${e.message}")
                    }
                }
                
                Log.d(TAG, "MediaStore扫描完成: 总图片=$totalImages, U盘图片=$usbImages, 找到目录=${folderMap.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "从MediaStore查找外置设备失败: ${e.message}", e)
        }
        
        return@withContext folderMap
    }
    
    /**
     * 获取外置设备的一级目录
     * @return 外置设备一级目录列表
     */
    suspend fun getExternalDeviceDirectories(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== 开始获取外置设备目录 ==========")
        
        // 方法1: 从MediaStore查找（最可靠，因为系统已经扫描过）
        val mediaStoreFolderMap = findExternalDeviceFoldersFromMediaStore()
        val mediaStoreFolders = mediaStoreFolderMap.map { (path, photos) ->
            PhotoFolder(
                path = path,
                name = File(path).name,
                photoCount = photos.size,
                coverPhoto = photos.firstOrNull()
            )
        }.sortedByDescending { it.photoCount }
        
        Log.d(TAG, "方法1(MediaStore)找到 ${mediaStoreFolders.size} 个目录")
        
        if (mediaStoreFolders.isNotEmpty()) {
            Log.d(TAG, "========== 外置设备目录获取完成(使用MediaStore) ==========")
            return@withContext mediaStoreFolders
        }
        
        // 方法2: 直接扫描文件系统
        val deviceDirs = mutableListOf<PhotoFolder>()
        val usbPaths = detectUsbPaths()
        
        Log.d(TAG, "方法2(文件扫描)检测到 ${usbPaths.size} 个USB路径")
        
        usbPaths.forEach { usbPath ->
            try {
                val dir = File(usbPath)
                Log.d(TAG, "扫描路径: $usbPath, exists=${dir.exists()}, canRead=${dir.canRead()}")
                
                if (dir.exists() && dir.isDirectory && dir.canRead()) {
                    // 只获取一级子目录
                    val subDirs = dir.listFiles { file -> file.isDirectory }
                    Log.d(TAG, "找到 ${subDirs?.size ?: 0} 个子目录")
                    
                    subDirs?.forEach { subDir ->
                        try {
                            Log.d(TAG, "检查子目录: ${subDir.name}")
                            // 递归统计该目录下的图片数量
                            val photoCount = countPhotosInDirectory(subDir)
                            Log.d(TAG, "子目录 ${subDir.name} 包含 $photoCount 张图片")
                            
                            if (photoCount > 0) {
                                // 获取第一张图片作为封面
                                val coverPhoto = getFirstPhotoInDirectory(subDir)
                                deviceDirs.add(
                                    PhotoFolder(
                                        path = subDir.absolutePath,
                                        name = subDir.name,
                                        photoCount = photoCount,
                                        coverPhoto = coverPhoto
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "处理外置设备目录失败: ${subDir.absolutePath}, ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "扫描外置设备路径失败: $usbPath, ${e.message}")
            }
        }
        
        Log.d(TAG, "方法2找到 ${deviceDirs.size} 个外置设备目录")
        Log.d(TAG, "========== 外置设备目录获取完成(使用文件扫描) ==========")
        
        return@withContext deviceDirs.sortedByDescending { it.photoCount }
    }
    
    /**
     * 获取系统根目录(/storage)下的所有目录
     * @return 目录列表
     */
    suspend fun getBrowseDirectories(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val directories = mutableListOf<PhotoFolder>()
        
        Log.d(TAG, "========== 开始浏览系统目录 ==========")
        
        try {
            val storageDir = File("/storage")
            Log.d(TAG, "检查/storage目录, exists=${storageDir.exists()}, canRead=${storageDir.canRead()}")
            
            if (storageDir.exists() && storageDir.isDirectory) {
                storageDir.listFiles { file -> file.isDirectory }?.forEach { dir ->
                    try {
                        val name = dir.name
                        val canRead = dir.canRead()
                        
                        Log.d(TAG, "发现目录: $name, canRead=$canRead")
                        
                        if (canRead) {
                            // 递归统计图片数量
                            val photoCount = countPhotosInDirectory(dir)
                            Log.d(TAG, "目录 $name 包含 $photoCount 张图片")
                            
                            if (photoCount > 0) {
                                val coverPhoto = getFirstPhotoInDirectory(dir)
                                directories.add(
                                    PhotoFolder(
                                        path = dir.absolutePath,
                                        name = name,
                                        photoCount = photoCount,
                                        coverPhoto = coverPhoto
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理目录失败: ${dir.name}, ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "浏览系统目录失败: ${e.message}", e)
        }
        
        Log.d(TAG, "找到 ${directories.size} 个可访问的目录")
        Log.d(TAG, "========== 系统目录浏览完成 ==========")
        
        return@withContext directories.sortedByDescending { it.photoCount }
    }
    
    /**
     * 只统计当前目录下的图片数量（不递归，跳过隐藏文件）
     */
    private fun countPhotosInDirectory(dir: File): Int {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return 0
        
        var count = 0
        try {
            // 只统计当前目录的图片（排除隐藏文件）
            dir.listFiles { file ->
                file.isFile &&
                !file.name.startsWith(".") &&
                SUPPORTED_EXTENSIONS.any {
                    file.extension.equals(it, ignoreCase = true)
                }
            }?.let { count = it.size }
            
            Log.d(TAG, "目录 ${dir.name} 包含 $count 张图片（不含隐藏文件）")
        } catch (e: Exception) {
            Log.e(TAG, "统计图片数量失败: ${dir.absolutePath}, ${e.message}")
        }
        return count
    }
    
    /**
     * 递归统计目录下所有图片数量（包括子目录）
     */
    private fun countAllPhotosInDirectory(dir: File): Int {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return 0
        
        var count = 0
        try {
            // 统计当前目录的图片
            dir.listFiles { file ->
                file.isFile && SUPPORTED_EXTENSIONS.any {
                    file.extension.equals(it, ignoreCase = true)
                }
            }?.let { count += it.size }
            
            // 递归统计子目录
            dir.listFiles { file -> file.isDirectory }?.forEach { subDir ->
                count += countAllPhotosInDirectory(subDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "递归统计图片数量失败: ${dir.absolutePath}, ${e.message}")
        }
        return count
    }
    
    /**
     * 获取目录下的第一张图片（跳过隐藏文件）
     */
    private fun getFirstPhotoInDirectory(dir: File): Photo? {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return null
        
        // 跳过隐藏目录
        if (dir.name.startsWith(".")) return null
        
        try {
            // 先查找当前目录（排除隐藏文件）
            dir.listFiles { file ->
                file.isFile &&
                !file.name.startsWith(".") &&
                SUPPORTED_EXTENSIONS.any {
                    file.extension.equals(it, ignoreCase = true)
                }
            }?.firstOrNull()?.let { file ->
                return Photo(
                    uri = Uri.fromFile(file),
                    path = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    dateAdded = file.lastModified() / 1000,
                    dateModified = file.lastModified() / 1000
                )
            }
            
            // 递归查找子目录（排除隐藏目录）
            dir.listFiles { file ->
                file.isDirectory && !file.name.startsWith(".")
            }?.forEach { subDir ->
                getFirstPhotoInDirectory(subDir)?.let { return it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取封面图片失败: ${dir.absolutePath}, ${e.message}")
        }
        return null
    }
    
    /**
     * 获取本地图片文件夹（不包括U盘）
     * @return 本地PhotoFolder列表
     */
    suspend fun getLocalPhotoFolders(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, MutableList<Photo>>()
        
        try {
            // 定义查询的列
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            
            // 按修改时间降序排列
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            
            // 查询所有图片
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
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
                        
                        // 过滤掉U盘路径
                        if (!isUsbPath(path)) {
                            // 获取文件夹路径
                            val folderPath = File(path).parent
                            if (folderPath != null) {
                                // 构建URI
                                val uri = ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id
                                )
                                
                                // 创建Photo对象
                                val photo = Photo(
                                    uri = uri,
                                    path = path,
                                    name = name,
                                    size = size,
                                    dateAdded = dateAdded,
                                    dateModified = dateModified
                                )
                                
                                // 添加到对应文件夹
                                if (!folderMap.containsKey(folderPath)) {
                                    folderMap[folderPath] = mutableListOf()
                                }
                                folderMap[folderPath]?.add(photo)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理图片时出错: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "找到 ${folderMap.size} 个本地图片文件夹")
        } catch (e: Exception) {
            Log.e(TAG, "获取本地图片文件夹失败: ${e.message}", e)
        }
        
        // 将Map转换为PhotoFolder列表
        val photoFolders = folderMap.map { (path, photos) ->
            val folderName = File(path).name
            PhotoFolder(
                path = path,
                name = folderName,
                photoCount = photos.size,
                coverPhoto = photos.firstOrNull()
            )
        }.sortedByDescending { it.photoCount }
        
        Log.d(TAG, "总共找到 ${photoFolders.size} 个本地图片文件夹")
        return@withContext photoFolders
    }
    
    /**
     * 获取所有包含图片的文件夹及其详细信息（包括U盘）
     * @return PhotoFolder列表
     */
    suspend fun getPhotoFoldersWithDetails(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, MutableList<Photo>>()
        
        // 首先扫描U盘中的文件夹
        val usbFolders = scanUsbFolders()
        
        // 将U盘文件夹添加到结果中
        val allFolders = usbFolders.toMutableList()
        
        try {
            
            // 定义查询的列
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED
            )
            
            // 按修改时间降序排列
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            
            // 查询所有图片
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
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
                        
                        // 获取文件夹路径
                        val folderPath = File(path).parent
                        if (folderPath != null) {
                            // 构建URI
                            val uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            
                            // 创建Photo对象
                            val photo = Photo(
                                uri = uri,
                                path = path,
                                name = name,
                                size = size,
                                dateAdded = dateAdded,
                                dateModified = dateModified
                            )
                            
                            // 添加到对应文件夹
                            if (!folderMap.containsKey(folderPath)) {
                                folderMap[folderPath] = mutableListOf()
                            }
                            folderMap[folderPath]?.add(photo)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理图片时出错: ${e.message}")
                    }
                }
            }
            
            Log.d(TAG, "找到 ${folderMap.size} 个图片文件夹")
        } catch (e: Exception) {
            Log.e(TAG, "获取图片文件夹详细信息失败: ${e.message}", e)
        }
        
        // 将Map转换为PhotoFolder列表（MediaStore的文件夹）
        val photoFolders = folderMap.map { (path, photos) ->
            val folderName = File(path).name
            PhotoFolder(
                path = path,
                name = folderName,
                photoCount = photos.size,
                coverPhoto = photos.firstOrNull() // 使用第一张图片作为封面
            )
        }
        
        // 合并MediaStore文件夹和U盘文件夹，去重
        allFolders.addAll(photoFolders)
        val uniqueFolders = allFolders.distinctBy { it.path }
            .sortedByDescending { it.photoCount } // 按图片数量降序排列
        
        Log.d(TAG, "总共找到 ${uniqueFolders.size} 个图片文件夹（包括 ${usbFolders.size} 个U盘文件夹）")
        return@withContext uniqueFolders
    }
}