# 更新日志

本项目基于上游 fork 后转为独立维护，版本号自 `1.0.0` 重新起算。
`0.1.8.x` 及更早的版本为上游记录，见文末「上游历史」。

## 未发布

### 新增

- 新增「布局锁定」设置：开启后将当前图标顺序固化为自定义顺序并持久化，新安装的应用自动追加到末尾，锁定期间排序方式不可修改；可在设置中关闭锁定时的提示

## 1.0.0

首个独立维护版本，功能基线为上游 `0.1.8.6`。

### 新增

- 新增应用排序设置
- 支持系统壁纸

### 变更

- 包名由 `cn.modificator.launcher` 全量重命名为 `com.wuming.einklauncher`，与上游版本相互独立、可共存安装
- 移除 About 页、崩溃页及源码注释中的上游作者署名与联系方式
- 接入 release 签名配置（`KeyStore.jks` + `keystore.properties`），文件缺失时自动跳过，不影响 Debug 构建

### 重构

- 重写 `EInkLauncherView`，抽取 `IconCache`，引入 Callback 模式
- 拆分 `LauncherAdapter`，View 与数据绑定职责分离
- 抽取 `AppItemBinder`，数据绑定与交互逻辑独立
- 用回调接口替代广播通知设置变更
- 重构设置持久化与生命周期处理
- 全量清理死代码

### 修复

- 修复 Android 12+ / 13+ 兼容性问题

### 性能

- 优化 Launcher 显示与图标加载性能

### 工程

- 版本基线重定为 `versionCode = 1` / `versionName = 1.0.0`
- 移除未被引用的死源集（`app/src/epd`、`app/src/home`）与第三方 jar（`app/src/libs`）
- 精简版本目录 `gradle/libs.versions.toml` 与 `gradle.properties`
- 构建产物 `app/release/mapping.txt`、`app/release/output.json` 移出版本库

## 上游历史

### 0.1.8.6

- fix custom wifi icon failed
- one key lock crash

### 0.1.8.5

- fix no custom wifi icon crash

### 0.1.8.4

- fix some devices can not show full setting
- lock screen icon name: `E-ink_Launcher.Lock.png`
- wifi icon name: `E-ink_Launcher.WifiOn.png`、`E-ink_Launcher.WifiOff.png`
