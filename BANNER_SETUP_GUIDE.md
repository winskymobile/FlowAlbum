# TV Banner图标设置指南

## 当前状态

✅ AndroidManifest.xml 已配置 `android:banner="@drawable/app_banner"`
⚠️ 当前使用的是XML drawable，需要替换为PNG图片

## 解决方案

### 步骤1：准备Banner图片

创建一张 **320 x 180 像素** 的PNG图片，命名为 `app_banner.png`

**设计要求**：
- 尺寸：320 x 180 像素（必须）
- 格式：PNG
- 内容：应用名称 "FlowAlbum" + 图标/装饰
- 背景：建议使用深色或渐变背景
- 文字：清晰易读，高对比度

### 步骤2：放置文件

将 `app_banner.png` 放到以下位置：

```
FlowAlbum/
└── app/
    └── src/
        └── main/
            └── res/
                └── drawable/
                    └── app_banner.png  ← 放在这里
```

### 步骤3：删除XML文件

删除现有的XML文件：
```bash
删除文件：app/src/main/res/drawable/app_banner.xml
```

或者在 Android Studio 中：
1. 找到 `res/drawable/app_banner.xml`
2. 右键 -> Delete
3. 确认删除

### 步骤4：重新构建

1. 在 Android Studio 中：Build -> Clean Project
2. 然后：Build -> Rebuild Project
3. 运行应用

## 在线Banner生成工具

如果没有设计工具，可以使用在线工具：

### 推荐工具：
1. **Canva** (https://www.canva.com)
   - 选择"自定义尺寸" -> 320 x 180 像素
   - 添加文字 "FlowAlbum"
   - 添加图标或装饰元素
   - 导出为PNG

2. **Figma** (https://www.figma.com)
   - 创建 320x180 的画布
   - 设计你的banner
   - 导出为PNG

3. **简单方案 - 纯色Banner**：
   使用任何图片编辑软件创建一个简单的banner：
   - 背景：蓝色渐变 (#2196F3 到 #1976D2)
   - 文字："FlowAlbum" 白色，居中
   - 尺寸：320 x 180 像素

## 示例Banner设计

### 方案1：简约风格
```
┌────────────────────────────────┐
│                                │
│          FlowAlbum            │
│       TV相册应用               │
│                                │
└────────────────────────────────┘
蓝色渐变背景 + 白色文字
```

### 方案2：图标+文字
```
┌────────────────────────────────┐
│  [📷图标]                       │
│           FlowAlbum            │
│                                │
└────────────────────────────────┘
深色背景 + 图标 + 应用名
```

## 验证Banner是否生效

### 方法1：在Android Studio中
1. 打开 `AndroidManifest.xml`
2. 找到 `android:banner="@drawable/app_banner"`
3. 按住 Cmd/Ctrl 点击 `@drawable/app_banner`
4. 应该能跳转到你的PNG文件

### 方法2：检查APK
```bash
unzip -l app-debug.apk | grep banner
# 应该看到：res/drawable/app_banner.png
```

### 方法3：在TV上测试
1. 完全卸载旧版本应用
2. 安装新的APK
3. 在TV主屏幕查看应用图标
4. 应该显示你的自定义banner

## 常见问题

### Q: 图标还是不显示
A: 检查清单：
- [ ] 文件名正确：app_banner.png（小写）
- [ ] 尺寸正确：320 x 180 像素
- [ ] 位置正确：res/drawable/ 目录
- [ ] 已删除同名XML文件
- [ ] 已Clean并Rebuild项目
- [ ] 已完全卸载旧版本应用

### Q: 图标显示模糊
A: 确保：
- 图片是320x180像素，不是其他尺寸
- 使用PNG格式，不是JPEG
- 设计时使用高质量素材

### Q: 图标被拉伸
A: Android TV会自动缩放banner，确保：
- 设计时保持320:180的宽高比
- 重要内容放在中央区域
- 边缘留有适当空白

## 临时解决方案

如果暂时没有设计工具，可以：

1. **使用应用图标**：
   ```xml
   <!-- 在 AndroidManifest.xml 中临时使用 -->
   android:banner="@mipmap/ic_launcher"
   ```

2. **使用纯色PNG**：
   创建一个320x180的纯色图片作为临时banner

## 需要帮助？

Banner图片必须由您自己提供，因为：
1. 这是应用的视觉标识
2. 需要符合您的品牌风格
3. 系统无法自动生成PNG图片文件

请按照上述步骤准备并放置 `app_banner.png` 文件！