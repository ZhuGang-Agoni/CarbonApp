
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
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

    // Google 服务
    implementation(libs.play.services.fitness)
    implementation("com.google.android.gms:play-services-auth:21.2.0")

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

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0-alpha03")


    // 强制使用特定版本的 core 库
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-ktx:1.16.0")
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