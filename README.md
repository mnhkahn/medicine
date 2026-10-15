# Medicine（药不能停）

Android 服药提醒应用，包名 `com.cyeam.medicine`。构建需要 JDK 17 或 21、Android SDK；仓库自带 Gradle Wrapper。

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleRelease
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`；Release APK 位于 `app/build/outputs/apk/release/app-release.apk`。

## 自动发布到蒲公英

`.github/workflows/android-release.yml` 在推送 `v1.0.2` 或 `1.0.2` 形式的 tag 时触发，也可以在 Actions 手动运行并填写版本。流程运行单元测试、签名构建 Release APK、上传蒲公英并等待发布完成；tag 发布还会创建 GitHub Release。两处共用从 Git 提交历史生成的更新说明。蒲公英下载页为 [药不能停下载页](https://www.pgyer.com/yaobunengting)，发布后 Actions 摘要会输出实际下载页。

在 GitHub 仓库 Settings → Secrets and variables → Actions 配置：

| Secret | 内容 |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | 当前正式签名 keystore 的 Base64（macOS：`base64 -i /path/to/release.jks`） |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 签名别名 |
| `KEY_PASSWORD` | 私钥密码 |
| `PGYER_API_KEY` | 当前蒲公英应用所属账户的 API Key |

必须沿用已安装正式包的签名，才能覆盖升级并保留数据。本地构建仍兼容 `local.properties` 的 `signing.storeFile`、`signing.storePassword`、`signing.keyAlias`、`signing.keyPassword`；环境变量 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 优先。

CI 的 `versionName` 取填写的版本或 tag（去掉开头的 `v`），`versionCode` 为 workflow run number + 1000，从 1001 开始，高于现有正式版的 2。后续发布保持同一工作流的递增序列；重跑同一次运行不会产生更高的 versionCode，发布新版本应触发新运行。本地可以使用 `-PreleaseVersionName=1.0.2 -PreleaseVersionCode=1001` 覆盖版本。

也可以本地上传已签名的 APK，脚本需要 Bash、curl、jq：

```sh
# 先通过环境安全注入 PGYER_API_KEY，再执行：
bash scripts/publish-pgyer.sh app/build/outputs/apk/release/app-release.apk /path/to/release-notes.md
```

蒲公英安装方式为公开安装，不限制下载起止时间。脚本使用获取凭证、上传、查询发布结果的三步流程，上传完成后仍会等待发布结果，不会把文件传输成功误判成可下载。

## 应用自动更新

应用启动后访问 `https://www.cyeam.com/api/apps/yaobunengting/update?versionCode=<当前版本>`。这里的服务端 appId 是 `yaobunengting`，不是仓库名 `medicine`。已核对现有服务端配置及线上接口：包名为 `com.cyeam.medicine`，蒲公英短链接为 `yaobunengting`；2026-10-15 接口正常返回当前版本 2（v1.0.1）。

发现新版后使用系统 DownloadManager 在非计费网络后台下载，禁止漫游；进度、目标版本和下载任务 ID 保存在本地，重开应用后恢复监控。下载完成后校验 APK 大小、包名、versionCode 和签名，校验通过才提供安装入口。回到前台会提示安装；首次需允许本应用安装未知应用，最终由系统确认安装。取消后可以从右上角“检查更新”重新打开状态、安装或失败重试。

客户端复用 `../homework` 的更新协议，不包含蒲公英 API Key。服务端 `../cyeam_web/conf/app_updates.json` 已有配置：

```json
{"id":"yaobunengting","packageName":"com.cyeam.medicine","shortcut":"yaobunengting","apiKeyEnv":"PGYER_API_KEY"}
```

网关的下载令牌有有效期；等待网络过久、下载失败或令牌过期时，在“检查更新”中点“重试”获取新地址并重建下载。网关不支持断点续传，网络中断后的下载可能需要重试。

## 首次启用

配置上述 GitHub Secrets，提交并推送代码后发布新 tag。已安装的 v1.0.1 尚无更新代码，需要先手动安装首个包含此功能的同签名新版，此后才能应用内升级。Debug 包与正式包的签名通常不同，校验会拒绝跨签名安装。
