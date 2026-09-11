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

**构建踩坑**：构建首次中断后残留的 `app/build` 会导致 `processDebugResources` 报
`Cannot access output property 'RClassOutputJar' ... R.jar does not exist`，
并非代码问题，`rm -rf app/build` 后重建即可。

## 待办

（暂无）
