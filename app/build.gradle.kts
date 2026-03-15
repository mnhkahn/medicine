// 1. 补充缺失的导入（核心修复点）
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.cyeam.medicine"
    compileSdk = 34

    // 2. 简化Properties引用（核心修复点：去掉java.util.前缀）
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        // 3. 简化FileInputStream引用（核心修复点：去掉java.io.前缀）
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    val storeFile = localProperties.getProperty("signing.storeFile")
    val storePassword = localProperties.getProperty("signing.storePassword")
    val keyAlias = localProperties.getProperty("signing.keyAlias")
    val keyPassword = localProperties.getProperty("signing.keyPassword")
    var baiduMTJKey = localProperties.getProperty("baidu.mtj.app.key")

    defaultConfig {
        applicationId = "com.cyeam.medicine"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "v1.0.1"

        androidResources {
            generateLocaleConfig = true
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }

        manifestPlaceholders["BAIDU_MTJ_APP_KEY"] = baiduMTJKey
    }

    signingConfigs {
        create("release") {
            storeFile?.let { this.storeFile = file(it) }
            storePassword?.let { this.storePassword = it }
            keyAlias?.let { this.keyAlias = it }
            keyPassword?.let { this.keyPassword = it }
        }
    }

    // 4. 修正buildTypes语法（Kotlin DSL规范）
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 补充：绑定签名配置（否则release包不会用自定义签名）
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // 基础库
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(files("libs/Baidu_Mtj_android_4.0.11.0.jar"))

    // Room 数据库
    val roomVersion = "2.4.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // ViewModel & LiveData
    val lifecycleVersion = "2.6.1"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // 测试库
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}