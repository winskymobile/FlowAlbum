
package com.example.flowalbum

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.net.Uri
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

/**
 * 幻灯片播放Activity
 * 这是应用的主界面，负责全屏显示图片并提供自动播放、手动控制等功能
 */
class SlideshowActivity : AppCompatActivity() {

    // UI组件
    private lateinit var imageViewCurrent: ImageView      // 当前显示的图片
    private lateinit var imageViewNext: ImageView         // 下一张图片（用于动画）
    private lateinit var progressBar: ProgressBar         // 加载进度条
    private lateinit var textLoading: TextView            // 加载提示文本
    private lateinit var textNoPhotos: TextView           // 无图片提示
    private lateinit var layoutControls: View             // 控制栏容器
    private lateinit var textPhotoInfo: TextView          // 图片信息文本
    private lateinit var btnPrevious: Button              // 上一张按钮
    private lateinit var btnPlayPause: Button             // 播放/暂停按钮
    private lateinit var btnNext: Button                  // 下一张按钮
    private lateinit var btnSettings: Button              // 设置按钮

    // 工具类实例
    private lateinit var photoLoader: PhotoLoader         // 图片加载器
    private lateinit var settingsManager: SettingsManager // 设置管理器
    private lateinit var animationHelper: AnimationHelper // 动画助手

    // 数据
    private val photoList = mutableListOf<Photo>()        // 图片列表
    private var currentIndex = 0                           // 当前图片索引

    // 播放控制
    private var isPlaying = false                          // 是否正在播放
    private var userPaused = false                         // 用户是否主动暂停（通过控制栏暂停按钮）
    private val handler = Handler(Looper.getMainLooper())  // 主线程Handler
    private var slideshowRunnable: Runnable? = null       // 幻灯片播放任务
    
    // 控制栏自动隐藏
    private var hideControlsRunnable: Runnable? = null    // 自动隐藏控制栏任务
    private val CONTROLS_HIDE_DELAY = 5000L                // 控制栏自动隐藏延迟（5秒）

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，加载图片
            loadPhotos()
        } else {
            // 权限被拒绝，显示提示
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slideshow)

        // 初始化工具类
        initializeHelpers()

        // 初始化UI组件
        initializeViews()

        // 设置点击监听器
        setupClickListeners()

        // 检查并请求权限
        checkAndRequestPermissions()
    }

    /**
     * 初始化工具类
     */
    private fun initializeHelpers() {
        photoLoader = PhotoLoader(this)
        settingsManager = SettingsManager(this)
        animationHelper = AnimationHelper()
    }

    /**
     * 初始化UI组件
     */
    private fun initializeViews() {
        imageViewCurrent = findViewById(R.id.imageViewCurrent)
        imageViewNext = findViewById(R.id.imageViewNext)
        progressBar = findViewById(R.id.progressBar)
        textLoading = findViewById(R.id.textLoading)
        textNoPhotos = findViewById(R.id.textNoPhotos)
        layoutControls = findViewById(R.id.layoutControls)
        textPhotoInfo = findViewById(R.id.textPhotoInfo)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext = findViewById(R.id.btnNext)
        btnSettings = findViewById(R.id.btnSettings)
        
        // 根据设置应用硬件加速
        if (settingsManager.isHardwareAcceleration()) {
            imageViewCurrent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            imageViewNext.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            imageViewCurrent.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            imageViewNext.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        
        // 根据设置应用缩放模式
        applyScaleType()
    }
    
    /**
     * 应用图片缩放模式
     */
    private fun applyScaleType() {
        val scaleType = if (settingsManager.isFitScreen()) {
            ImageView.ScaleType.FIT_CENTER  // 全屏展示（缩放以适应屏幕）
        } else {
            ImageView.ScaleType.CENTER_INSIDE  // 保持原始尺寸（不放大，只在需要时缩小）
        }
        imageViewCurrent.scaleType = scaleType
        imageViewNext.scaleType = scaleType
    }

    /**
     * 设置按钮点击监听器
     */
    private fun setupClickListeners() {
        // 上一张按钮
        btnPrevious.setOnClickListener {
            resetAutoHideTimer()
            showPreviousPhoto()
        }

        // 播放/暂停按钮
        btnPlayPause.setOnClickListener {
            resetAutoHideTimer()
            togglePlayPause()
        }

        // 下一张按钮
        btnNext.setOnClickListener {
            resetAutoHideTimer()
            showNextPhoto()
        }

        // 设置按钮
        btnSettings.setOnClickListener {
            cancelAutoHideTimer()
            layoutControls.visibility = View.GONE
            showSettingsDialog()
        }

        // 点击图片区域显示/隐藏控制栏
        imageViewCurrent.setOnClickListener {
            toggleControlsVisibility()
        }
    }

    /**
     * 检查并请求存储权限
     */
    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // 已有权限，直接加载图片
                loadPhotos()
            }
            else -> {
                // 请求权限
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    /**
     * 加载图片列表
     */
    private fun loadPhotos() {
        // 显示加载状态
        showLoading(true)

        lifecycleScope.launch {
            try {
                // 从设置中获取文件夹路径
                val folderPath = settingsManager.getFolderPath()
                
                // 加载图片
                val photos = if (folderPath != null) {
                    photoLoader.loadPhotosFromDirectory(folderPath)
                } else {
                    photoLoader.loadPhotosFromFolder(null)
                }

                // 更新图片列表
                photoList.clear()
                photoList.addAll(photos)

                // 隐藏加载状态
                showLoading(false)

                if (photoList.isEmpty()) {
                    // 没有找到图片
                    showNoPhotosMessage()
                } else {
                    // 显示第一张图片
                    currentIndex = 0
                    displayCurrentPhoto()
                    updatePhotoInfo()

                    // 根据设置决定是否自动播放（只有当自动播放开关打开且用户未主动暂停时才自动播放）
                    if (settingsManager.isAutoPlay() && !userPaused) {
                        startSlideshow()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("加载图片失败: ${e.message}")
            }
        }
    }

    /**
     * 显示当前图片
     */
    private fun displayCurrentPhoto() {
        if (photoList.isEmpty() || currentIndex >= photoList.size) return

        val photo = photoList[currentIndex]
        
        // 构建Glide请求 - 始终使用最高画质
        var requestBuilder = Glide.with(this)
            .load(photo.uri)
            .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL, com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
            .encodeQuality(100)  // 最高编码质量
            .dontAnimate()  // 禁用动画
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.DATA)  // 缓存原始数据
            .skipMemoryCache(false)
        
        // 根据真彩色开关设置色彩格式
        if (settingsManager.isHighQualityMode()) {
            requestBuilder = requestBuilder.format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)  // 32位真彩色
        } else {
            requestBuilder = requestBuilder.format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)  // 16位色彩（节省内存）
        }
        
        requestBuilder.into(imageViewCurrent)
        
        // 应用色彩饱和度效果
        applySaturationEffect(imageViewCurrent)
    }
    
    /**
     * 应用色彩饱和度效果
     */
    private fun applySaturationEffect(imageView: ImageView) {
        val saturation = settingsManager.getColorSaturation()
        
        // 如果饱和度为0，则不应用任何效果
        if (saturation == 0) {
            imageView.colorFilter = null
            return
        }
        
        // 创建饱和度矩阵
        val saturationMatrix = android.graphics.ColorMatrix()
        // 饱和度：0 = 1.0 (不处理), 1-5 = 1.2到2.0 (增强饱和度)
        val saturationValue = 1.0f + (saturation * 0.2f)  // 1=1.2, 2=1.4, 3=1.6, 4=1.8, 5=2.0
        saturationMatrix.setSaturation(saturationValue)
        
        // 应用饱和度滤镜
        imageView.colorFilter = android.graphics.ColorMatrixColorFilter(saturationMatrix)
    }

    /**
     * 显示下一张图片（带动画）
     */
    private fun showNextPhoto() {
        if (photoList.isEmpty()) return

        // 保存当前播放状态
        val wasPlaying = isPlaying
        
        // 停止自动播放
        stopSlideshow()

        // 移动到下一张
        currentIndex = (currentIndex + 1) % photoList.size
        
        // 显示图片并应用动画
        displayPhotoWithAnimation()

        // 如果之前是播放状态，继续播放
        if (wasPlaying) {
            startSlideshow()
        }
    }

    /**
     * 显示上一张图片
     */
    private fun showPreviousPhoto() {
        if (photoList.isEmpty()) return

        // 保存当前播放状态
        val wasPlaying = isPlaying
        
        // 停止自动播放
        stopSlideshow()

        // 移动到上一张
        currentIndex = if (currentIndex > 0) currentIndex - 1 else photoList.size - 1
        
        // 显示图片并应用动画
        displayPhotoWithAnimation()

        // 如果之前是播放状态，继续播放
        if (wasPlaying) {
            startSlideshow()
        }
    }

    /**
     * 带动画地显示图片
     */
    private fun displayPhotoWithAnimation() {
        if (photoList.isEmpty() || currentIndex >= photoList.size) return

        val photo = photoList[currentIndex]
        
        // 获取动画类型
        val animationType = if (settingsManager.isRandomAnimation()) {
            // 随机选择一个动画（不包括RANDOM本身）
            val types = AnimationHelper.AnimationType.values().filter { it != AnimationHelper.AnimationType.RANDOM }
            types.random()
        } else {
            settingsManager.getAnimationTypeEnum()
        }
        
        // 构建Glide请求 - 始终使用最高画质
        var glideRequest = Glide.with(this)
            .load(photo.uri)
            .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL, com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
            .encodeQuality(100)  // 最高编码质量
            .dontAnimate()  // 禁用动画
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.DATA)  // 缓存原始数据
            .skipMemoryCache(false)
        
        // 根据真彩色开关设置色彩格式
        if (settingsManager.isHighQualityMode()) {
            glideRequest = glideRequest.format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)  // 32位真彩色
        } else {
            glideRequest = glideRequest.format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)  // 16位色彩
        }
        
        // 先加载图片
        glideRequest.into(imageViewCurrent)
        
        // 应用色彩饱和度效果
        applySaturationEffect(imageViewCurrent)
        
        // 根据动画类型应用不同效果
        when (animationType) {
            AnimationHelper.AnimationType.FADE -> {
                // 淡入淡出
                imageViewCurrent.alpha = 0f
                imageViewCurrent.animate().alpha(1f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.SLIDE_LEFT -> {
                // 从右向左滑入
                imageViewCurrent.translationX = imageViewCurrent.width.toFloat()
                imageViewCurrent.alpha = 1f
                imageViewCurrent.animate().translationX(0f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.SLIDE_RIGHT -> {
                // 从左向右滑入
                imageViewCurrent.translationX = -imageViewCurrent.width.toFloat()
                imageViewCurrent.alpha = 1f
                imageViewCurrent.animate().translationX(0f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.SLIDE_UP -> {
                // 从下向上滑入
                imageViewCurrent.translationY = imageViewCurrent.height.toFloat()
                imageViewCurrent.alpha = 1f
                imageViewCurrent.animate().translationY(0f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.SLIDE_DOWN -> {
                // 从上向下滑入
                imageViewCurrent.translationY = -imageViewCurrent.height.toFloat()
                imageViewCurrent.alpha = 1f
                imageViewCurrent.animate().translationY(0f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.ZOOM_IN -> {
                // 从小到大
                imageViewCurrent.scaleX = 0.3f
                imageViewCurrent.scaleY = 0.3f
                imageViewCurrent.alpha = 0f
                imageViewCurrent.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.ZOOM_OUT -> {
                // 从大到小
                imageViewCurrent.scaleX = 1.5f
                imageViewCurrent.scaleY = 1.5f
                imageViewCurrent.alpha = 0f
                imageViewCurrent.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(800).start()
            }
            AnimationHelper.AnimationType.ROTATE -> {
                // 旋转进入
                imageViewCurrent.rotation = -90f
                imageViewCurrent.alpha = 0f
                imageViewCurrent.animate().rotation(0f).alpha(1f).setDuration(800).start()
            }
            else -> {
                // 默认淡入淡出
                imageViewCurrent.alpha = 0f
                imageViewCurrent.animate().alpha(1f).setDuration(800).start()
            }
        }

        // 更新信息
        updatePhotoInfo()
    }

    /**
     * 开始幻灯片自动播放
     */
    private fun startSlideshow() {
        isPlaying = true
        btnPlayPause.text = getString(R.string.pause)

        // 创建定时任务
        slideshowRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    // 移动到下一张
                    currentIndex = (currentIndex + 1) % photoList.size
                    displayPhotoWithAnimation()
                    
                    // 计算下一次切换的延迟：间隔时间 + 动画时长（800ms）
                    val animationDuration = 800L
                    val nextDelay = settingsManager.getInterval() + animationDuration
                    
                    // 继续下一次调度
                    handler.postDelayed(this, nextDelay)
                }
            }
        }

        // 开始第一次调度，使用相同的间隔保证一致性
        handler.postDelayed(slideshowRunnable!!, settingsManager.getInterval())
    }

    /**
     * 停止幻灯片自动播放
     */
    private fun stopSlideshow() {
        isPlaying = false
        btnPlayPause.text = getString(R.string.play)
        
        slideshowRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    /**
     * 切换播放/暂停状态
     */
    private fun togglePlayPause() {
        if (photoList.isEmpty()) return

        if (isPlaying) {
            // 用户主动暂停
            userPaused = true
            stopSlideshow()
        } else {
            // 用户主动播放，清除暂停标记
            userPaused = false
            startSlideshow()
        }
    }

    /**
     * 更新图片信息文本
     */
    private fun updatePhotoInfo() {
        // 获取当前播放速度（秒）
        val intervalSeconds = settingsManager.getIntervalSeconds()
        
        // 获取当前动画效果名称
        val animationName = if (settingsManager.isRandomAnimation()) {
            // 随机动画
            animationHelper.getAnimationName(AnimationHelper.AnimationType.RANDOM)
        } else {
            // 指定动画类型
            animationHelper.getAnimationName(settingsManager.getAnimationTypeEnum())
        }
        
        // 格式化信息：第 X 张，共 Y 张 | Z秒 | 动画名称
        val info = getString(
            R.string.photo_info_with_settings,
            currentIndex + 1,
            photoList.size,
            intervalSeconds,
            animationName
        )
        textPhotoInfo.text = info
    }

    /**
     * 切换控制栏可见性
     */
    private fun toggleControlsVisibility() {
        if (layoutControls.visibility == View.VISIBLE) {
            // 隐藏控制栏
            layoutControls.visibility = View.GONE
            cancelAutoHideTimer()
        } else {
            // 显示控制栏
            layoutControls.visibility = View.VISIBLE
            startAutoHideTimer()
            // 聚焦到播放/暂停按钮
            btnPlayPause.post {
                btnPlayPause.requestFocus()
            }
        }
    }
    
    /**
     * 启动控制栏自动隐藏倒计时
     */
    private fun startAutoHideTimer() {
        // 先取消之前的倒计时
        cancelAutoHideTimer()
        
        // 创建新的倒计时任务
        hideControlsRunnable = Runnable {
            if (layoutControls.visibility == View.VISIBLE) {
                layoutControls.visibility = View.GONE
            }
        }
        
        // 3秒后执行
        handler.postDelayed(hideControlsRunnable!!, CONTROLS_HIDE_DELAY)
    }
    
    /**
     * 取消控制栏自动隐藏倒计时
     */
    private fun cancelAutoHideTimer() {
        hideControlsRunnable?.let {
            handler.removeCallbacks(it)
        }
        hideControlsRunnable = null
    }
    
    /**
     * 重置控制栏自动隐藏倒计时
     * 用于用户操作按钮后，重新开始计时
     */
    private fun resetAutoHideTimer() {
        if (layoutControls.visibility == View.VISIBLE) {
            startAutoHideTimer()
        }
    }

    /**
     * 显示设置对话框 - Tab形式，实时保存
     */
    private fun showSettingsDialog() {
        // 保存播放状态，但不暂停（如果自动播放开关打开）
        val wasPlaying = isPlaying
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_FlowAlbum_Dialog)
            .setView(dialogView)
            .create()

        // 获取Tab标签
        val tabAnimation = dialogView.findViewById<Button>(R.id.tabAnimation)
        val tabDisplay = dialogView.findViewById<Button>(R.id.tabDisplay)
        val tabPhotos = dialogView.findViewById<Button>(R.id.tabPhotos)
        val tabAbout = dialogView.findViewById<Button>(R.id.tabAbout)
        
        // 获取内容页面
        val pageAnimation = dialogView.findViewById<View>(R.id.pageAnimation)
        val pageDisplay = dialogView.findViewById<View>(R.id.pageDisplay)
        val pagePhotos = dialogView.findViewById<View>(R.id.pagePhotos)
        val pageAbout = dialogView.findViewById<View>(R.id.pageAbout)
        
        // 获取间隔设置控件
        val textInterval = dialogView.findViewById<TextView>(R.id.textInterval)
        val btnDecreaseInterval = dialogView.findViewById<Button>(R.id.btnDecreaseInterval)
        val btnIncreaseInterval = dialogView.findViewById<Button>(R.id.btnIncreaseInterval)
        
        // 获取动画设置控件
        val radioGroupAnimation = dialogView.findViewById<RadioGroup>(R.id.radioGroupAnimation)
        
        // 获取动画设置控件 - 作为普通Button而不是RadioButton
        val btnFade = dialogView.findViewById<RadioButton>(R.id.radioFade)
        val btnSlideLeft = dialogView.findViewById<RadioButton>(R.id.radioSlideLeft)
        val btnSlideRight = dialogView.findViewById<RadioButton>(R.id.radioSlideRight)
        val btnSlideUp = dialogView.findViewById<RadioButton>(R.id.radioSlideUp)
        val btnSlideDown = dialogView.findViewById<RadioButton>(R.id.radioSlideDown)
        val btnZoomIn = dialogView.findViewById<RadioButton>(R.id.radioZoomIn)
        val btnZoomOut = dialogView.findViewById<RadioButton>(R.id.radioZoomOut)
        val btnRotate = dialogView.findViewById<RadioButton>(R.id.radioRotate)
        val btnRandom = dialogView.findViewById<RadioButton>(R.id.radioRandom)
        
        // 所有动画按钮列表
        val animationButtons = listOf(btnFade, btnSlideLeft, btnSlideRight, btnSlideUp, btnSlideDown,
                                      btnZoomIn, btnZoomOut, btnRotate, btnRandom)
        
        // 获取显示设置控件
        val layoutAutoPlay = dialogView.findViewById<LinearLayout>(R.id.layoutAutoPlay)
        val layoutHighQuality = dialogView.findViewById<LinearLayout>(R.id.layoutHighQuality)
        val layoutHardwareAccel = dialogView.findViewById<LinearLayout>(R.id.layoutHardwareAccel)
        val layoutFitScreen = dialogView.findViewById<LinearLayout>(R.id.layoutFitScreen)
        val switchAutoPlay = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchAutoPlay)
        val switchHighQuality = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchHighQuality)
        val switchHardwareAccel = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchHardwareAccel)
        val switchFitScreen = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchFitScreen)
        // 获取色彩饱和度控件
        val textSaturationValue = dialogView.findViewById<TextView>(R.id.textSaturationValue)
        val seekBarSaturation = dialogView.findViewById<android.widget.SeekBar>(R.id.seekBarSaturation)
        
        // 获取关闭按钮
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        // 获取选择图片页面控件
        val btnLocalPhotos = dialogView.findViewById<Button>(R.id.btnLocalPhotos)
        val btnExternalDevices = dialogView.findViewById<Button>(R.id.btnExternalDevices)
        val recyclerViewFolders = dialogView.findViewById<RecyclerView>(R.id.recyclerViewFolders)
        
        // 预先初始化RecyclerView，避免切换tab时闪烁
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        recyclerViewFolders.layoutManager = gridLayoutManager
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerViewFolders.addItemDecoration(GridSpacingItemDecoration(3, spacing, true))
        
        // 获取关于页面控件
        val textVersion = dialogView.findViewById<TextView>(R.id.textVersion)
        val btnCheckUpdate = dialogView.findViewById<Button>(R.id.btnCheckUpdate)
        
        // 设置版本号
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            textVersion.text = "版本 ${packageInfo.versionName}"
        } catch (e: Exception) {
            textVersion.text = "版本 1.0"
        }
        
        // Tab切换函数
        fun switchTab(selectedTab: Button, selectedPage: View) {
            // 取消所有tab的选中状态
            tabAnimation.isSelected = false
            tabDisplay.isSelected = false
            tabPhotos.isSelected = false
            tabAbout.isSelected = false
            
            // 设置当前tab为选中状态
            selectedTab.isSelected = true
            
            // 隐藏所有页面
            pageAnimation.visibility = View.GONE
            pageDisplay.visibility = View.GONE
            pagePhotos.visibility = View.GONE
            pageAbout.visibility = View.GONE
            
            // 显示选中的页面
            selectedPage.visibility = View.VISIBLE
            
            // 如果切换到选择图片页面，加载本地文件夹列表
            if (selectedPage == pagePhotos && recyclerViewFolders.adapter == null) {
                loadLocalFoldersInDialog(recyclerViewFolders, dialog, wasPlaying)
            }
            
            // 请求焦点到选中的Tab
            selectedTab.requestFocus()
        }
        
        // Tab点击事件
        tabAnimation.setOnClickListener { switchTab(tabAnimation, pageAnimation) }
        tabDisplay.setOnClickListener { switchTab(tabDisplay, pageDisplay) }
        tabPhotos.setOnClickListener { switchTab(tabPhotos, pagePhotos) }
        tabAbout.setOnClickListener { switchTab(tabAbout, pageAbout) }
        
        // 默认选中第一个tab（动画效果）
        tabAnimation.isSelected = true

        // 初始化当前设置值
        var currentIntervalSeconds = settingsManager.getIntervalSeconds()
        textInterval.text = getString(R.string.interval_seconds, currentIntervalSeconds)

        // 先取消所有按钮的选中状态（避免RadioGroup默认行为）
        animationButtons.forEach { it.isChecked = false }
        
        // 根据当前动画类型选中对应的按钮
        val currentType = settingsManager.getAnimationTypeEnum()
        when (currentType) {
            AnimationHelper.AnimationType.FADE -> btnFade.isChecked = true
            AnimationHelper.AnimationType.SLIDE_LEFT -> btnSlideLeft.isChecked = true
            AnimationHelper.AnimationType.SLIDE_RIGHT -> btnSlideRight.isChecked = true
            AnimationHelper.AnimationType.SLIDE_UP -> btnSlideUp.isChecked = true
            AnimationHelper.AnimationType.SLIDE_DOWN -> btnSlideDown.isChecked = true
            AnimationHelper.AnimationType.ZOOM_IN -> btnZoomIn.isChecked = true
            AnimationHelper.AnimationType.ZOOM_OUT -> btnZoomOut.isChecked = true
            AnimationHelper.AnimationType.ROTATE -> btnRotate.isChecked = true
            AnimationHelper.AnimationType.RANDOM -> btnRandom.isChecked = true
        }
        
        // 动画按钮点击处理 - 手动实现单选和保存
        fun selectAnimation(selectedButton: RadioButton, animationType: AnimationHelper.AnimationType) {
            // 取消所有按钮的选中状态
            animationButtons.forEach { it.isChecked = false }
            // 选中当前按钮
            selectedButton.isChecked = true
            // 立即保存并刷新图片信息
            settingsManager.setAnimationType(animationType)
            settingsManager.setRandomAnimation(animationType == AnimationHelper.AnimationType.RANDOM)
            updatePhotoInfo()
        }
        
        // 为每个动画按钮设置点击监听
        btnFade.setOnClickListener { selectAnimation(btnFade, AnimationHelper.AnimationType.FADE) }
        btnSlideLeft.setOnClickListener { selectAnimation(btnSlideLeft, AnimationHelper.AnimationType.SLIDE_LEFT) }
        btnSlideRight.setOnClickListener { selectAnimation(btnSlideRight, AnimationHelper.AnimationType.SLIDE_RIGHT) }
        btnSlideUp.setOnClickListener { selectAnimation(btnSlideUp, AnimationHelper.AnimationType.SLIDE_UP) }
        btnSlideDown.setOnClickListener { selectAnimation(btnSlideDown, AnimationHelper.AnimationType.SLIDE_DOWN) }
        btnZoomIn.setOnClickListener { selectAnimation(btnZoomIn, AnimationHelper.AnimationType.ZOOM_IN) }
        btnZoomOut.setOnClickListener { selectAnimation(btnZoomOut, AnimationHelper.AnimationType.ZOOM_OUT) }
        btnRotate.setOnClickListener { selectAnimation(btnRotate, AnimationHelper.AnimationType.ROTATE) }
        btnRandom.setOnClickListener { selectAnimation(btnRandom, AnimationHelper.AnimationType.RANDOM) }

        // 设置开关状态
        switchAutoPlay.isChecked = settingsManager.isAutoPlay()
        switchHighQuality.isChecked = settingsManager.isHighQualityMode()
        switchHardwareAccel.isChecked = settingsManager.isHardwareAcceleration()
        switchFitScreen.isChecked = settingsManager.isFitScreen()
        
        // 初始化色彩饱和度显示
        val currentSaturation = settingsManager.getColorSaturation()
        textSaturationValue.text = currentSaturation.toString()
        seekBarSaturation.progress = currentSaturation

        // 色彩饱和度SeekBar监听
        seekBarSaturation.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                textSaturationValue.text = progress.toString()
                settingsManager.setColorSaturation(progress)
                applySaturationEffect(imageViewCurrent)
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        // 色彩饱和度SeekBar按键监听 - 防止焦点跳出
        seekBarSaturation.setOnKeyListener { view, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val currentProgress = seekBarSaturation.progress
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (currentProgress <= 0) {
                            // 已经在最小值，消费事件防止焦点跳出
                            true
                        } else {
                            false  // 允许正常调整
                        }
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (currentProgress >= 5) {
                            // 已经在最大值，消费事件防止焦点跳出
                            true
                        } else {
                            false  // 允许正常调整
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
        }

        // 间隔调整按钮 - 实时保存
        btnDecreaseInterval.setOnClickListener {
            if (currentIntervalSeconds > 1) {
                currentIntervalSeconds--
                textInterval.text = getString(R.string.interval_seconds, currentIntervalSeconds)
                settingsManager.setIntervalSeconds(currentIntervalSeconds)
                updatePhotoInfo()
            }
        }

        btnIncreaseInterval.setOnClickListener {
            if (currentIntervalSeconds < 30) {
                currentIntervalSeconds++
                textInterval.text = getString(R.string.interval_seconds, currentIntervalSeconds)
                settingsManager.setIntervalSeconds(currentIntervalSeconds)
                updatePhotoInfo()
            }
        }

        // 自动播放开关 - 点击整个容器切换状态
        layoutAutoPlay.setOnClickListener {
            switchAutoPlay.isChecked = !switchAutoPlay.isChecked
        }
        
        // 自动播放开关监听 - 实时保存
        switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setAutoPlay(isChecked)
        }

        // 高质量模式开关 - 点击整个容器切换状态
        layoutHighQuality.setOnClickListener {
            switchHighQuality.isChecked = !switchHighQuality.isChecked
        }
        
        // 高质量模式开关监听 - 实时保存并刷新显示
        switchHighQuality.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setHighQualityMode(isChecked)
            // 直接重新加载当前图片，不清除缓存
            displayCurrentPhoto()
        }

        // 硬件加速开关 - 点击整个容器切换状态
        layoutHardwareAccel.setOnClickListener {
            switchHardwareAccel.isChecked = !switchHardwareAccel.isChecked
        }
        
        // 硬件加速开关监听 - 实时保存
        switchHardwareAccel.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setHardwareAcceleration(isChecked)
            if (isChecked) {
                imageViewCurrent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                imageViewNext.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                imageViewCurrent.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                imageViewNext.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
        }
        
        // 适应屏幕开关 - 点击整个容器切换状态
        layoutFitScreen.setOnClickListener {
            switchFitScreen.isChecked = !switchFitScreen.isChecked
        }
        
        // 适应屏幕开关监听 - 实时保存并应用
        switchFitScreen.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setFitScreen(isChecked)
            // 立即应用缩放模式
            applyScaleType()
            // 重新显示当前图片以应用新的缩放模式
            displayCurrentPhoto()
        }

        // 本地图片按钮
        btnLocalPhotos.setOnClickListener {
            loadLocalFoldersInDialog(recyclerViewFolders, dialog, wasPlaying)
        }
        
        // 外置设备按钮
        btnExternalDevices.setOnClickListener {
            loadExternalDevicesInDialog(recyclerViewFolders, dialog, wasPlaying)
        }
        
        // 检测更新按钮（关于Tab）
        btnCheckUpdate.setOnClickListener {
            // 显示检测更新提示
            val checkingDialog = AlertDialog.Builder(this)
                .setMessage(getString(R.string.checking_update))
                .setCancelable(false)
                .create()
            checkingDialog.show()
            
            // 使用UpdateChecker检测更新
            val updateChecker = UpdateChecker(this)
            lifecycleScope.launch {
                val updateInfo = updateChecker.checkForUpdate()
                
                checkingDialog.dismiss()
                
                when {
                    // 检测出错
                    updateInfo.errorMessage != null -> {
                        AlertDialog.Builder(this@SlideshowActivity)
                            .setTitle(getString(R.string.update_check_failed))
                            .setMessage(updateInfo.errorMessage)
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    // 有新版本
                    updateInfo.hasUpdate -> {
                        val message = getString(
                            R.string.update_version_info,
                            updateInfo.latestVersion,
                            updateInfo.latestTimestamp,
                            updateInfo.currentVersion,
                            updateInfo.currentTimestamp
                        )
                        
                        AlertDialog.Builder(this@SlideshowActivity)
                            .setTitle(getString(R.string.update_available))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.update_download)) { dialog, _ ->
                                dialog.dismiss()
                                // 使用内置下载功能（自动处理代理站链接）
                                startDownload(updateChecker, updateInfo.downloadUrl, updateInfo.fileName)
                            }
                            .setNegativeButton(getString(R.string.update_later)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    // 已是最新版本
                    else -> {
                        val message = getString(
                            R.string.current_version_info,
                            updateInfo.currentVersion,
                            updateInfo.currentTimestamp
                        )
                        
                        AlertDialog.Builder(this@SlideshowActivity)
                            .setTitle(getString(R.string.check_update))
                            .setMessage("${getString(R.string.no_update)}\n\n$message")
                            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }
        }

        // 关闭按钮
        btnClose.setOnClickListener {
            dialog.dismiss()
            // 对话框关闭后不需要特殊处理，因为打开时没有暂停播放
        }

        dialog.show()
        
        // 设置对话框高度为屏幕高度的80%
        dialog.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val dialogHeight = (screenHeight * 0.8).toInt()
            
            window.setLayout(
                android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                dialogHeight
            )
        }
    }

    /**
     * 在设置对话框中加载本地文件夹列表
     */
    private fun loadLocalFoldersInDialog(
        recyclerView: RecyclerView,
        parentDialog: AlertDialog,
        wasPlaying: Boolean
    ) {
        // 显示加载提示（RecyclerView已在对话框创建时初始化）
        val progressDialog = AlertDialog.Builder(this)
            .setMessage(getString(R.string.loading_local_folders))
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // 加载本地文件夹列表
        lifecycleScope.launch {
            try {
                val folders = photoLoader.getLocalPhotoFolders()
                
                progressDialog.dismiss()
                
                if (folders.isEmpty()) {
                    Toast.makeText(this@SlideshowActivity, "未找到本地图片文件夹", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 设置适配器
                val adapter = FolderAdapter(folders) { folder ->
                    // 点击文件夹
                    parentDialog.dismiss()
                    loadPhotosFromSelectedFolder(folder.path)
                    // 重新加载图片时，清除用户暂停标记
                    userPaused = false
                }
                recyclerView.adapter = adapter
                
                Toast.makeText(
                    this@SlideshowActivity,
                    "找到 ${folders.size} 个本地文件夹",
                    Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@SlideshowActivity, "加载本地文件夹失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 在设置对话框中加载外置设备目录
     */
    private fun loadExternalDevicesInDialog(
        recyclerView: RecyclerView,
        parentDialog: AlertDialog,
        wasPlaying: Boolean
    ) {
        // 显示加载提示（RecyclerView已在对话框创建时初始化）
        val progressDialog = AlertDialog.Builder(this)
            .setMessage(getString(R.string.loading_external_devices))
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // 加载外置设备目录
        lifecycleScope.launch {
            try {
                val devices = photoLoader.getExternalDeviceDirectories()
                
                progressDialog.dismiss()
                
                if (devices.isEmpty()) {
                    Toast.makeText(this@SlideshowActivity, getString(R.string.no_external_devices), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 设置适配器
                val adapter = FolderAdapter(devices) { device ->
                    // 点击外置设备目录
                    parentDialog.dismiss()
                    loadPhotosFromSelectedFolder(device.path)
                    // 重新加载图片时，清除用户暂停标记
                    userPaused = false
                }
                recyclerView.adapter = adapter
                
                Toast.makeText(
                    this@SlideshowActivity,
                    "找到 ${devices.size} 个外置设备目录",
                    Toast.LENGTH_SHORT
                ).show()
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@SlideshowActivity, "加载外置设备失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示/隐藏加载状态
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        textLoading.visibility = if (show) View.VISIBLE else View.GONE
        imageViewCurrent.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * 显示无图片提示
     */
    private fun showNoPhotosMessage() {
        textNoPhotos.visibility = View.VISIBLE
        imageViewCurrent.visibility = View.GONE
        layoutControls.visibility = View.GONE
    }

    /**
     * 显示权限被拒绝提示
     */
    private fun showPermissionDeniedMessage() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage(getString(R.string.permission_required))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .show()
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 显示文件夹选择对话框
     */
    private fun showFolderSelectionDialog() {
        // 暂停播放
        val wasPlaying = isPlaying
        if (isPlaying) {
            stopSlideshow()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_folder_selection, null)
        val dialog = AlertDialog.Builder(this, R.style.Theme_FlowAlbum_Dialog)
            .setView(dialogView)
            .create()

        // 获取对话框中的控件
        val btnAllPhotos = dialogView.findViewById<Button>(R.id.btnAllPhotos)
        val recyclerViewFolders = dialogView.findViewById<RecyclerView>(R.id.recyclerViewFolders)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        // 设置RecyclerView为宫格布局（3列）
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        recyclerViewFolders.layoutManager = gridLayoutManager
        
        // 添加宫格间距装饰器
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerViewFolders.addItemDecoration(GridSpacingItemDecoration(3, spacing, true))
        
        // 显示加载提示
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("正在加载文件夹...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // 加载文件夹列表
        lifecycleScope.launch {
            try {
                val folders = photoLoader.getPhotoFoldersWithDetails()
                
                progressDialog.dismiss()
                
                if (folders.isEmpty()) {
                    Toast.makeText(this@SlideshowActivity, "未找到图片文件夹", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    if (wasPlaying) {
                        startSlideshow()
                    }
                    return@launch
                }
                
                // 设置适配器
                val adapter = FolderAdapter(folders) { folder ->
                    // 点击文件夹
                    dialog.dismiss()
                    loadPhotosFromSelectedFolder(folder.path)
                    if (wasPlaying) {
                        startSlideshow()
                    }
                }
                recyclerViewFolders.adapter = adapter
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@SlideshowActivity, "加载文件夹失败: ${e.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                if (wasPlaying) {
                    startSlideshow()
                }
            }
        }
        
        // 所有图片按钮
        btnAllPhotos.setOnClickListener {
            dialog.dismiss()
            loadPhotosFromSelectedFolder(null)
            if (wasPlaying) {
                startSlideshow()
            }
        }
        
        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
            if (wasPlaying) {
                startSlideshow()
            }
        }
        
        dialog.show()
    }
    
    /**
     * 从选定的文件夹加载图片
     */
    private fun loadPhotosFromSelectedFolder(folderPath: String?) {
        // 显示加载状态
        showLoading(true)
        
        // 清除Glide缓存（切换播放目录时）
        Glide.get(this@SlideshowActivity).clearMemory()
        Thread {
            Glide.get(this@SlideshowActivity).clearDiskCache()
        }.start()
        
        lifecycleScope.launch {
            try {
                // 保存选择的文件夹路径
                settingsManager.setFolderPath(folderPath)
                
                // 加载图片
                val photos = if (folderPath != null) {
                    photoLoader.loadPhotosFromFolder(folderPath)
                } else {
                    photoLoader.loadPhotosFromFolder(null)
                }
                
                // 更新图片列表
                photoList.clear()
                photoList.addAll(photos)
                
                // 隐藏加载状态
                showLoading(false)
                
                if (photoList.isEmpty()) {
                    showNoPhotosMessage()
                    Toast.makeText(this@SlideshowActivity, "该文件夹中没有图片", Toast.LENGTH_SHORT).show()
                } else {
                    // 显示第一张图片
                    currentIndex = 0
                    displayCurrentPhoto()
                    updatePhotoInfo()
                    
                    // 隐藏无图片提示
                    textNoPhotos.visibility = View.GONE
                    imageViewCurrent.visibility = View.VISIBLE
                    
                    val folderName = if (folderPath != null) {
                        java.io.File(folderPath).name
                    } else {
                        "所有图片"
                    }
                    Toast.makeText(
                        this@SlideshowActivity,
                        "已加载 ${photoList.size} 张图片 ($folderName)",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 根据自动播放设置决定是否开始播放（只有当自动播放开关打开且用户未主动暂停时才自动播放）
                    if (settingsManager.isAutoPlay() && !userPaused && !isPlaying) {
                        startSlideshow()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("加载图片失败: ${e.message}")
            }
        }
    }
    
    /**
     * 开始下载更新并显示进度
     */
    private fun startDownload(updateChecker: UpdateChecker, downloadUrl: String, fileName: String) {
        // 开始下载
        val downloadId = updateChecker.downloadApk(downloadUrl, fileName)
        
        if (downloadId == -1L) {
            Toast.makeText(this, "启动下载失败", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 创建自定义进度对话框
        val progressDialogView = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        val progressText = progressDialogView.findViewById<TextView>(R.id.textDownloadStatus)
        val progressBar = progressDialogView.findViewById<ProgressBar>(R.id.progressBarDownload)
        val btnCancel = progressDialogView.findViewById<Button>(R.id.btnCancelDownload)
        val btnBackground = progressDialogView.findViewById<Button>(R.id.btnBackgroundDownload)
        
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("下载更新")
            .setView(progressDialogView)
            .setCancelable(false)
            .create()
        
        // 取消下载按钮
        btnCancel.setOnClickListener {
            updateChecker.cancelDownload(downloadId)
            progressDialog.dismiss()
            Toast.makeText(this, "已取消下载", Toast.LENGTH_SHORT).show()
        }
        
        // 后台下载按钮
        btnBackground.setOnClickListener {
            progressDialog.dismiss()
            Toast.makeText(this, "下载将在后台继续", Toast.LENGTH_SHORT).show()
        }
        
        progressDialog.show()
        
        // 定时查询下载进度
        val progressHandler = Handler(Looper.getMainLooper())
        val progressRunnable = object : Runnable {
            override fun run() {
                val result = updateChecker.queryDownloadProgress(downloadId)
                
                if (result != null) {
                    val (progress, statusText) = result
                    progressText.text = statusText
                    progressBar.progress = progress
                    
                    // 如果下载完成或失败，停止查询
                    if (statusText.contains("完成")) {
                        progressDialog.dismiss()
                        
                        // 下载完成，自动打开安装
                        val fileUri = updateChecker.getDownloadedFileUri(downloadId)
                        if (fileUri != null) {
                            installApk(fileUri)
                        } else {
                            Toast.makeText(
                                this@SlideshowActivity,
                                "下载完成，但无法打开安装程序",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    } else if (statusText.contains("失败")) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@SlideshowActivity,
                            statusText,
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }
                
                // 继续查询
                progressHandler.postDelayed(this, 500)
            }
        }
        
        // 开始查询进度
        progressHandler.postDelayed(progressRunnable, 500)
        
        // 对话框关闭时移除回调
        progressDialog.setOnDismissListener {
            progressHandler.removeCallbacks(progressRunnable)
        }
    }
    
    /**
     * 安装 APK
     */
    private fun installApk(apkUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法打开安装程序: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 处理遥控器按键事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // 方向键左：仅用于控制栏按钮焦点切换
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                super.onKeyDown(keyCode, event)
            }
            // 方向键右：仅用于控制栏按钮焦点切换
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                super.onKeyDown(keyCode, event)
            }
            // 方向键上：显示控制栏
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (layoutControls.visibility != View.VISIBLE) {
                    layoutControls.visibility = View.VISIBLE
                    startAutoHideTimer()
                    // 聚焦到播放/暂停按钮
                    btnPlayPause.post {
                        btnPlayPause.requestFocus()
                    }
                    true
                } else {
                    resetAutoHideTimer()
                    super.onKeyDown(keyCode, event)
                }
            }
            // 方向键下：隐藏控制栏
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (layoutControls.visibility == View.VISIBLE) {
                    layoutControls.visibility = View.GONE
                    cancelAutoHideTimer()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            // 确认键：显示控制栏（与向上键行为一致）
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (layoutControls.visibility != View.VISIBLE) {
                    layoutControls.visibility = View.VISIBLE
                    startAutoHideTimer()
                    // 聚焦到播放/暂停按钮
                    btnPlayPause.post {
                        btnPlayPause.requestFocus()
                    }
                    true
                } else {
                    resetAutoHideTimer()
                    super.onKeyDown(keyCode, event)
                }
            }
            // 菜单键：打开设置
            KeyEvent.KEYCODE_MENU -> {
                cancelAutoHideTimer()
                showSettingsDialog()
                true
            }
            // 返回键：如果控制栏显示则隐藏，否则退出
            KeyEvent.KEYCODE_BACK -> {
                if (layoutControls.visibility == View.VISIBLE) {
                    layoutControls.visibility = View.GONE
                    cancelAutoHideTimer()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止播放并清理资源
        stopSlideshow()
        cancelAutoHideTimer()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        // Activity暂停时停止播放，但不标记为用户主动暂停
        // 这样当重新进入时，如果自动播放开关打开，会自动恢复播放
        if (isPlaying) {
            stopSlideshow()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Activity恢复时，如果自动播放开关打开且用户未主动暂停，自动开始播放
        if (settingsManager.isAutoPlay() && !userPaused && photoList.isNotEmpty() && !isPlaying) {
            startSlideshow()
        }
    }
}