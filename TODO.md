# TODO

## 必须备份（丢失将无法再发布升级包）

- `KeyStore.jks` —— release 签名密钥库
- `keystore.properties` —— 密钥库口令

两者均已被 `.gitignore` 排除，**不会进入版本库**，请务必另行备份（密码管理器 / 私有网盘）。
密钥库一旦丢失，已安装该应用的设备将再也无法接收后续升级包（相同 `applicationId` 的升级必须使用同一把私钥签名）。

## 待验证：编译

包名重命名与签名配置均**尚未经过编译验证**，需补做。

```bash
# 仅编译 Java 源码（快速验证）
./gradlew :app:compileDebugJavaWithJavac --console=plain

# 打包 Debug
./gradlew :app:assembleDebug

# 打包 Release（验证签名配置是否生效）
./gradlew :app:assembleRelease
```

本机环境：`ANDROID_HOME=D:\Android\SDK`
记录时间：2026-09-11

## 待办

- [ ] 重定版本号基线（当前 `versionCode = 30` / `versionName = "0.1.8.6"` 沿用上游编号，建议从 `1` / `1.0.0` 起）
- [ ] 删除死源集 `app/src/epd/`、`app/src/home/`（`build.gradle.kts` 未声明任何 productFlavors/sourceSets）
- [ ] 删除无引用文件 `app/src/libs/yotadevice_sdk-3.7.12.jar`
- [ ] 精简 `gradle/libs.versions.toml`（声明的 compose / navigation3 / kotlin 等依赖实际未使用）
- [ ] 从版本库移除已跟踪的构建产物 `app/release/mapping.txt`、`app/release/output.json`
- [ ] 重写 `CHANGELOG.md`（仍只有上游历史记录）

## 已完成

- [x] 包名 `cn.modificator.launcher` → `com.wuming.einklauncher` 全量重命名
- [x] 清理 About 页、崩溃页及源码注释中的上游作者署名与联系方式
- [x] `README.md`：加入对上游项目与原作者 Modificator 的致谢，移除失效的上游发布渠道说明
- [x] 接入 release 签名配置（`KeyStore.jks` + `keystore.properties`，文件缺失时自动跳过，不影响 Debug 构建）
