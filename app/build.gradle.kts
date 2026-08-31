import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 内置 Kotlin 支持，无需 kotlin-android 插件；
    // Compose 编译器随 Kotlin 版本走，单独应用 compose-compiler 插件。
    alias(libs.plugins.compose.compiler)
    // P6：kotlinx-serialization 编译器插件（JSON DTO 解析），与 compose-compiler 同款应用方式。
    alias(libs.plugins.kotlin.serialization)
}

// Release 签名配置：从 keystore/keystore.properties 读取（已加入 .gitignore）
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) {
        f.inputStream().use { stream -> load(stream) }
    }
}

android {
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/${keystoreProps.getProperty("storeFile", "asoul-release.jks")}")
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    namespace = "com.example.asoul"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.asoul"
        // 支持 Android 8.0+（API 26）：java.time 在 API 26 原生可用，无需脱糖
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 优化：代码收缩/优化/混淆 + 资源压缩（AGP 9 默认优化管线）
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        // java.time API（LocalDate/LocalTime）在 minSdk 26 原生可用
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.activity.compose)

    // P6 网络层：OkHttp GET 静态 JSON + kotlinx-serialization 解析
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Compose BOM 统一版本管理
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
