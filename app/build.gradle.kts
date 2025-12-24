plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.flowalbum"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.flowalbum"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // 核心库
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // TV Leanback 库
    implementation("androidx.leanback:leanback:1.0.0")
    
    // Activity 库
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // 图片加载库 Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // ViewPager2 用于图片轮播
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // ConstraintLayout 用于复杂布局
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Material Design 组件
    implementation("com.google.android.material:material:1.11.0")
    
    // 协程支持（用于异步操作）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}