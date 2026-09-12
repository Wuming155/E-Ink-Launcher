# AGENTS.md

面向 AI 编码代理的项目说明与强制规则。

## 构建规则（必须遵守）

**每次构建完成后，必须接着编译稳定版（release）。**

- 任何一次构建（Debug、单测、lint 等）成功结束后，都要追加执行 `./gradlew assembleRelease`，不得以任何理由跳过。
- 若 release 构建因缺少 `KeyStore.jks` / `keystore.properties` 而跳过签名，属于正常现象（未签名包仍会产出），照常报告结果即可，不算失败。
- 汇报构建结果时，需同时说明 Debug 与 Release 两个产物的状态和路径，路径必须写**完整绝对路径**（如 `D:\GithubWorkplace\E-Ink-Launcher\app\build\outputs\apk\debug\app-debug.apk`），不得使用相对路径。
