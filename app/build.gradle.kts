plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize") // 启用Parcelize支持
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
    //开启视图绑定
    viewBinding{
        enable=true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.play.services.fitness)
//    implementation(libs.androidx.camera.core)
//    implementation(libs.androidx.navigation.fragment.ktx)
//    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0") // 图片加载
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("com.tencent:mmkv:1.2.13")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
//    implementation ("com.amap.api:location:6.4.0")       // 定位SDK
    implementation("com.amap.api:search:9.7.0")
    implementation("com.amap.api:3dmap:9.8.2")
//    implementation("com.amap.api:map:7.9.0")
    implementation(files("libs/core-3.5.1.jar"))

     implementation("com.google.android.gms:play-services-fitness:21.2.0")
     implementation("com.google.android.gms:play-services-auth:21.2.0")
}