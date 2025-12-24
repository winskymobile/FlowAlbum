# USB外置存储设备系统级自动识别升级

## 修改概述

已将PhotoLoader中的USB设备检测从**硬编码路径方式**升级为**系统级自动识别方案**，不再依赖预定义的挂载路径列表。

## 主要改进

### 1. 移除硬编码路径
- **删除**: `USB_MOUNT_PATHS` 常量列表
- **原因**: 不同Android设备的USB挂载路径差异很大，硬编码方式无法适应所有设备

### 2. 增强 `detectStorageVolumes()` 方法
采用三层API策略：

#### Android 7.0+ (API 24+)
```kotlin
val storageVolumes = storageManager.storageVolumes
storageVolumes.forEach { volume ->
    val isRemovable = volume.isRemovable
    val state = volume.state
    if (state == Environment.MEDIA_MOUNTED && isRemovable) {
        // 添加可移除存储设备
    }
}
```

#### Android 7.0以下
使用反射调用隐藏API：
```kotlin
val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")
val volumeList = getVolumeList.invoke(storageManager) as Array<*>
// 通过反射获取path、state、isRemovable属性
```

### 3. 升级 `detectUsbPaths()` 方法
实现多种检测方式的组合：

#### 方法1: StorageManager API（最可靠）
- 使用系统服务直接获取可移除存储卷信息
- 支持所有Android版本

#### 方法2: Environment API（Android 4.4+）
```kotlin
val externalDirs = context.getExternalFilesDirs(null)
externalDirs.forEach { dir ->
    val isRemovable = Environment.isExternalStorageRemovable(dir)
    val state = Environment.getExternalStorageState(dir)
    if (isRemovable && state == Environment.MEDIA_MOUNTED) {
        // 提取存储根路径
    }
}
```

#### 方法3: 文件系统扫描（补充）
- 扫描 `/storage` 目录
- 自动排除系统内置存储（emulated、self、sdcard）
- 验证目录可读性

### 4. 新增 `extractStorageRootPath()` 方法
从应用私有目录提取存储设备根路径：
```
输入: /storage/1234-5678/Android/data/com.example.app/files
输出: /storage/1234-5678
```

### 5. 优化 `isUsbPath()` 方法
- 首先检查是否在系统检测到的外置存储路径中
- 排除内置存储（/emulated/, /sdcard/, /data/）
- 支持UUID格式匹配（FAT32和扩展格式）

## 技术优势

### ✅ 自动适应
- 不依赖设备厂商的挂载路径约定
- 支持各种Android版本（包括定制ROM）

### ✅ 可靠性高
- 使用Android官方StorageManager服务
- 多种检测方法互相补充
- 完整的错误处理和日志记录

### ✅ 兼容性强
- Android 7.0+ 使用公开API
- Android 7.0以下使用反射方案
- Android 4.4+ 使用Environment API增强

### ✅ 性能优化
- 使用Set去重避免重复路径
- 及时返回检测结果
- 详细的日志便于调试

## 使用场景

此升级使应用能够：
1. **自动识别所有类型的外置存储**：U盘、SD卡、OTG设备
2. **适配不同设备**：无论厂商如何定制系统
3. **动态响应**：热插拔设备时能正确识别
4. **提高用户体验**：无需手动配置或猜测路径

## 日志输出示例

```
D/PhotoLoader: ========== 开始系统级检测外置存储设备 ==========
D/PhotoLoader: 存储卷[API24+]: path=/storage/1234-5678, state=mounted, removable=true
D/PhotoLoader: ✓ 添加可移除存储: /storage/1234-5678
D/PhotoLoader: ✓ StorageManager API检测到 1 个外置存储设备
D/PhotoLoader:   - /storage/1234-5678
D/PhotoLoader: 外部存储: /storage/1234-5678/Android/data/..., removable=true, state=mounted
D/PhotoLoader: ✓ 通过Environment添加外置存储根路径: /storage/1234-5678
D/PhotoLoader: ========== 系统级检测完成: 找到 1 个外置存储设备 ==========
D/PhotoLoader:   ✓ /storage/1234-5678
```

## 注意事项

1. **权限要求**：仍需 `READ_EXTERNAL_STORAGE` 权限
2. **Android 10+**：需要 `requestLegacyExternalStorage` 或使用Scoped Storage
3. **测试建议**：在不同品牌设备上测试以确保兼容性

## 后续建议

1. 添加USB设备插拔监听（BroadcastReceiver）
2. 实现存储设备变化的实时通知
3. 添加设备类型识别（USB、SD卡等）
4. 优化大容量设备的扫描性能