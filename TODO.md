# TODO

## 必须备份（丢失将无法再发布升级包）

- `KeyStore.jks` —— release 签名密钥库
- `keystore.properties` —— 密钥库口令

两者均已被 `.gitignore` 排除，**不会进入版本库**，请务必另行备份（密码管理器 / 私有网盘）。
密钥库一旦丢失，已安装该应用的设备将再也无法接收后续升级包（相同 `applicationId` 的升级必须使用同一把私钥签名）。

## 环境备忘

```bash
ANDROID_HOME=D:\Android\SDK
```

**代理配置**（`~/.gradle/gradle.properties`）：必须 `http` 与 `https` 都配，只配 `http` 会导致 Gradle wrapper 下载分发包超时。

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

## 构建验证（已完成，2026-09-11）

```bash
./gradlew :app:assembleDebug     # BUILD SUCCESSFUL in 17s，30 tasks
./gradlew :app:assembleRelease   # BUILD SUCCESSFUL in 3m 5s，含 R8 混淆 + 资源压缩 + lintVital
```

产物核验：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk` —— 已签名，证书 `CN=Wuming155`，有效期至 2056-09-03
- 反查 APK：`package: name='com.wuming.einklauncher'`、`application-label: 'E-Ink Launcher'`

**踩坑**：构建首次中断后残留的 `app/build` 会导致 `processDebugResources` 报
`Cannot access output property 'RClassOutputJar' ... R.jar does not exist`，
并非代码问题，`rm -rf app/build` 后重建即可。

## 待办

- [ ] 重定版本号基线（当前 `versionCode = 30` / `versionName = "0.1.8.6"` 沿用上游编号，建议从 `1` / `1.0.0` 起）
- [ ] 删除死源集 `app/src/epd/`、`app/src/home/`（`build.gradle.kts` 未声明任何 productFlavors/sourceSets）
- [ ] 删除无引用文件 `app/src/libs/yotadevice_sdk-3.7.12.jar`
- [ ] 精简 `gradle/libs.versions.toml`（声明的 compose / navigation3 / kotlin 等依赖实际未使用）
- [ ] 从版本库移除已跟踪的构建产物 `app/release/mapping.txt`、`app/release/output.json`
- [ ] 重写 `CHANGELOG.md`（仍只有上游历史记录）
- [ ] 清理 `gradle.properties` 中上游遗留的无效配置（已注释的 1080 端口代理、多个已废弃的 `android.*` 开关）

## 已完成

- [x] 包名 `cn.modificator.launcher` → `com.wuming.einklauncher` 全量重命名
- [x] 清理 About 页、崩溃页及源码注释中的上游作者署名与联系方式
- [x] `README.md`：加入对上游项目与原作者 Modificator 的致谢，移除失效的上游发布渠道说明
- [x] 接入 release 签名配置（`KeyStore.jks` + `keystore.properties`，文件缺失时自动跳过，不影响 Debug 构建）
- [x] Debug / Release 构建验证通过，APK 包名与签名均已核验
