
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize") // 启用Parcelize支持
    id("kotlin-kapt") // 添加kapt插件支持
}

android {
    namespace = "com.zg.carbonapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zg.carbonapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        // 添加此配置解决 AndroidX 和 Support 库冲突
        configurations.all {
            resolutionStrategy {
                force("androidx.core:core:1.16.0")
                force("androidx.core:core-ktx:1.16.0")
            }
        }
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

    viewBinding {
        enable = true
    }

    packagingOptions {
        // 方法1：保留其中一个库的文件（推荐，需确认保留哪个）
        pickFirst ("lib/arm64-v8a/libc++_shared.so")  // 只保留第一个找到的文件
        // 如果有其他架构（如armeabi-v7a、x86等）也冲突，需一并添加
        pickFirst ("lib/armeabi-v7a/libc++_shared.so")
        pickFirst ("lib/x86/libc++_shared.so")
        pickFirst ("lib/x86_64/libc++_shared.so")
    }


}

dependencies {
    // 基础 Android 组件
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)

    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.camera.view)
//    implementation(libs.androidx.room.runtime.android)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")

    // ARCore 和 Sceneform
    implementation("com.google.ar:core:1.31.0")
    implementation("com.google.ar.sceneform:core:1.17.1")
    implementation("com.google.ar.sceneform.ux:sceneform-ux:1.17.1")

    // 地图与定位 - 彻底解决 Support 库冲突
    implementation(libs.play.services.maps)



    implementation("com.amap.api:search:9.7.0") {
        exclude(group = "com.android.support")
        exclude(group = "androidx.core") // 额外排除 androidx.core
    }
    implementation("com.amap.api:3dmap:9.8.2") {
        exclude(group = "com.android.support")
        exclude(group = "androidx.core") // 额外排除 androidx.core
    }

    implementation ("com.google.android.material:material:1.11.0")

        // 其他依赖...
        implementation ("com.jakewharton.threetenabp:threetenabp:1.4.6")

// 通知和工作管理
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.core:core-ktx:1.8.0")
    // Google 服务
    implementation(libs.play.services.fitness)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")

    // 数据处理
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
    implementation("com.tencent:mmkv:1.2.13")

    // UI 组件
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "androidx.core") // 排除 Glide 可能引入的 core
    }
    kapt("com.github.bumptech.glide:compiler:4.16.0")


    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Coil 图片加载
    implementation("io.coil-kt:coil:2.4.0")

    // 二维码扫描 - 统一 ZXing 版本
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        exclude(group = "com.google.zxing")
        exclude(group = "androidx.core") // 额外排除 androidx.core
    }
    implementation("com.google.zxing:core:3.5.3")
    // 百度地图 的 依赖
//    implementation("com.baidu.lbsyun:BaiduMapSDK_Map:7.6.4")

    implementation("com.baidu.lbsyun:BaiduMapSDK_Search:7.6.4")
    implementation("com.baidu.lbsyun:BaiduMapSDK_Location:9.6.4")
    implementation("com.baidu.lbsyun:BaiduMapSDK_Util:7.6.4")
    implementation ("com.baidu.lbsyun:NaviTts:3.2.13")

    implementation("com.baidu.lbsyun:BaiduMapSDK_Map-AllNavi:7.6.4")
    implementation("com.airbnb.android:lottie:6.1.0")

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0-alpha03")

    implementation("com.google.android.gms:play-services-fitness:21.2.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // CameraX - 使用版本目录中的统一版本
    implementation(libs.androidx.camera.core)
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation(libs.androidx.camera.view)

    // ML Kit
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // OpenCV (可选，暂时注释掉，因为可能没有对应的仓库)
    // implementation("org.opencv:opencv-android:4.8.0")


    // 强制使用特定版本的 core 库
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-ktx:1.16.0")

//    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    // pytorch模型
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
}

// 全局解决依赖冲突
configurations.all {
    resolutionStrategy {
        // 强制使用 AndroidX 并排除所有 Support 库
        force("androidx.core:core:1.16.0")
        force("androidx.core:core-ktx:1.16.0")

        // 排除所有 Support 库
        exclude(group = "com.android.support")

        // 处理重复类错误
        failOnVersionConflict()

        // 优先使用较新版本的 AndroidX 库
        preferProjectModules()
    }
}