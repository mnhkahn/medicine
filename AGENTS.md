# 项目构建指南

本文档提供了如何为Medicine项目构建测试包和正式包的详细步骤。

## 环境准备

在构建项目之前，请确保您的开发环境已经配置好以下工具：

1. **JDK 8 或更高版本**
2. **Android Studio**（推荐最新版本）
3. **Gradle**（项目已经包含Gradle包装器，无需单独安装）

## 构建测试包

测试包（Debug Build）用于开发和测试阶段，包含调试信息，不进行代码混淆。

### 命令行构建

1. 执行以下命令构建测试包：
   ```bash
   ./gradlew assembleDebug
   ```

2. 构建成功后，APK文件会生成在以下位置：
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Android Studio构建

1. 打开Android Studio，加载Medicine项目
2. 在菜单栏中选择 **Build > Build Bundle(s) / APK(s) > Build APK(s)**
3. 构建完成后，Android Studio会提示APK生成位置

## 构建正式包

正式包（Release Build）用于发布到应用商店，会进行代码混淆和优化。

### 命令行构建

1. 执行以下命令构建正式包：
   ```bash
   ./gradlew assembleRelease
   ```

2. 构建成功后，APK文件会生成在以下位置：
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

### Android Studio构建

1. 打开Android Studio，加载Medicine项目
2. 在菜单栏中选择 **Build > Build Bundle(s) / APK(s) > Build APK(s)**
3. 构建完成后，Android Studio会提示APK生成位置

## 签名配置

正式包需要进行签名才能发布到应用商店。在构建正式包之前，请确保您已经配置了签名信息。

### 配置签名信息

1. 在项目的 `app/build.gradle.kts` 文件中添加签名配置：

   ```kotlin
   android {
       // ...
       signingConfigs {
           create("release") {
               storeFile = file("your-keystore.jks")
               storePassword = "your-store-password"
               keyAlias = "your-key-alias"
               keyPassword = "your-key-password"
           }
       }
       
       buildTypes {
           release {
               signingConfig = signingConfigs["release"]
               // ...
           }
       }
   }
   ```

2. 为了安全起见，建议将签名信息存储在环境变量中，而不是直接硬编码在构建文件中：

   ```kotlin
   android {
       // ...
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_FILE") ?: "your-keystore.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your-store-password"
               keyAlias = System.getenv("KEY_ALIAS") ?: "your-key-alias"
               keyPassword = System.getenv("KEY_PASSWORD") ?: "your-key-password"
           }
       }
   }
   ```

## 构建参数

### 调试构建参数

- **minifyEnabled**: false（不进行代码混淆）
- **debuggable**: true（可调试）
- **shrinkResources**: false（不压缩资源）

### 发布构建参数

- **minifyEnabled**: true（进行代码混淆）
- **debuggable**: false（不可调试）
- **shrinkResources**: true（压缩资源）
- **proguardFiles**: 应用ProGuard规则

## 构建常见问题

### 构建失败

如果构建失败，请检查以下几点：

1. **JDK版本**：确保使用JDK 8或更高版本
2. **依赖项**：确保所有依赖项都能正确下载
3. **签名配置**：确保签名配置正确
4. **权限**：确保项目具有所需的权限

### 构建速度慢

如果构建速度较慢，可以尝试以下方法：

1. **启用Gradle缓存**：在 `gradle.properties` 文件中添加 `org.gradle.caching=true`
2. **启用并行构建**：在 `gradle.properties` 文件中添加 `org.gradle.parallel=true`
3. **增加Gradle堆内存**：在 `gradle.properties` 文件中添加 `org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8`

## 总结

通过本文档，您应该能够成功构建Medicine项目的测试包和正式包。如果您遇到任何问题，请参考Android官方文档或咨询开发团队。
