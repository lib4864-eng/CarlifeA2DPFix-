plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.carlifeaudioroute"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.carlifeaudioroute"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // 关闭测试编译，避免缺失测试库报错
    testOptions {
        unitTests.all { it.enabled = false }
    }
}

tasks.withType<Test> {
    enabled = false
}

dependencies {
    // Xposed API
    compileOnly("de.robv.android.xposed:api:82")
    
    // 基础 UI 依赖 (保留一个 Activity 作为模块图标外壳)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
