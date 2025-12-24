# 如何添加自定义Banner图标

## 图标要求

Android TV Banner 图标必须满足以下规格：
- **尺寸**：320 x 180 像素
- **格式**：PNG（推荐）或JPG
- **文件名**：app_banner.png
- **设计建议**：
  - 使用清晰的应用名称或logo
  - 避免过多文字
  - 使用高对比度颜色
  - 确保在大屏幕上清晰可见

## 添加步骤

### 方法1：使用Android Studio（推荐）

1. **打开项目**
   - 在 Android Studio 中打开 FlowAlbum 项目

2. **找到 drawable 目录**
   - 在左侧项目树中找到：`app/src/main/res/drawable`
   - 如果没有该目录，右键点击 `res` -> New -> Directory -> 输入 `drawable`

3. **添加图片文件**
   - 将您的 `app_banner.png` 文件复制到该目录
   - 或者右键点击 `drawable` -> Show in Finder/Explorer
   - 将文件直接拖入该文件夹

4. **删除旧的XML文件（可选）**
   - 删除 `app/src/main/res/drawable/app_banner.xml`
   - 因为PNG文件会自动覆盖同名的XML

5. **重新构建项目**
   - 点击 Build -> Clean Project
   - 然后点击 Build -> Rebuild Project

### 方法2：手动添加

1. **定位到目录**
   ```
   FlowAlbum/app/src/main/res/drawable/
   ```

2. **放置文件**
   - 将 `app_banner.png` 复制到该目录
   - 确保文件名正确（区分大小写）

3. **验证文件**
   - 检查文件大小是否为 320x180 像素
   - 检查文件格式是否为 PNG

4. **删除XML文件（如果存在）**
   ```
   删除：app/src/main/res/drawable/app_banner.xml
   ```

## 文件放置位置

```
FlowAlbum/
├── app/
│   └── src/
│       └── main/
│           └── res/
│               └── drawable/
│                   └── app_banner.png  ← 放在这里！
```

## 验证Banner是否生效

1. **在Android Studio中查看**
   - 打开 `AndroidManifest.xml`
   - 找到 `android:banner="@drawable/app_banner"`
   - 按住 Ctrl/Cmd 点击 `@drawable/app_banner`
   - 应该能跳转到您的PNG文件

2. **构建并安装**
   - 重新构建APK
   - 卸载旧版本应用
   - 安装新版本
   - 在TV主屏幕查看图标

## 如果图标仍然不显示

### 检查清单：
- [ ] 文件名必须是 `app_banner.png`（小写，下划线）
- [ ] 图片尺寸必须是 320x180 像素
- [ ] 文件必须在 `drawable` 目录（不是 `drawable-xxhdpi` 等）
- [ ] 已删除同名的 XML 文件
- [ ] 已完全卸载旧版本应用
- [ ] 已重启 Android Studio
- [ ] 已 Clean 和 Rebuild 项目

### 调试命令：
```bash
# 查看APK中是否包含banner
unzip -l app-debug.apk | grep banner

# 应该能看到类似输出：
# res/drawable/app_banner.png
```

## 设计Banner的工具推荐

- **在线工具**：Canva, Figma
- **桌面软件**：Photoshop, GIMP, Paint.NET
- **Android工具**：Android Studio 的 Image Asset Studio

## Banner设计示例

推荐的设计元素：
1. **背景**：纯色或渐变
2. **Logo/图标**：应用的主要标识
3. **应用名称**：清晰易读的字体
4. **点缀**：简单的装饰元素

避免：
- ❌ 过多文字
- ❌ 复杂的图案
- ❌ 低对比度
- ❌ 模糊的图片

## 需要帮助？

如果按照上述步骤操作后仍有问题：
1. 检查 Android Studio 的 Build 输出是否有错误
2. 确认文件大小（应该几十KB左右）
3. 尝试使用不同的PNG图片测试
4. 重启 Android TV 设备

---

**提示**：PNG文件会自动替换XML drawable，无需修改任何代码！