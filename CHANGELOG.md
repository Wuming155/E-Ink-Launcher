# 更新日志

本项目基于上游 fork 后转为独立维护，版本号自 `1.0.0` 重新起算。
`0.1.8.x` 及更早的版本为上游记录，见文末「上游历史」。

## 未发布

### 新增

- 新增「布局锁定」设置：开启后将当前图标顺序固化为自定义顺序并持久化，新安装的应用自动追加到末尾，锁定期间排序方式不可修改；可在设置中关闭锁定时的提示
- 布局锁定期间同步关闭「管理应用」入口与桌面长按管理弹窗（隐藏/卸载），避免取消隐藏使已隐藏应用重新显示、或隐藏现有应用破坏锁定布局
- 新增应用「重命名」：桌面长按应用 → 重命名，可自定义桌面显示名称，输入为空时恢复原始名称
- 新增「布局调整」模式：设置中点击后回到桌面，长按选中应用、再点击另一应用即交换位置，点击已选中的应用取消，桌面「完成」按钮退出；调整期间临时按自定义顺序排列，结果持久化到自定义顺序（配合布局锁定可长期固化）
- 应用名标签行数改为按页动态统一：图标尺寸保持不变，取本页最长名称所需行数（并与用户设置、可用空间可容纳行数取小）套用到整页，同一行图标保持水平对齐，名称在图标下方换行、超出时省略号截断，不再溢出单元格遮挡相邻图标或被裁剪；调大字体后网格立即按新字号重新排版

### 变更

- 精简「帮助 & 关于」弹窗：仅保留应用名、版本号与上游致谢，移除冗长的功能列表

### 移除

- 移除 FTP「网络传书」功能及 Apache FtpServer 依赖：该功能以匿名登录对外网开放整个外部存储的读写权限，且服务组件可被任意第三方应用静默拉起，存在高危安全风险
- 移除未使用的 FileProvider 声明（原配置将整个外部存储根目录设为可共享路径）
- 移除零引用权限：`INTERNET`、`RESTART_PACKAGES`、`DELETE_PACKAGES`、`WAKE_LOCK`、`ACCESS_NETWORK_STATE`；存储权限增加 `maxSdkVersion="32"`
- 移除「显示 WiFi 名字」设置项及 `ACCESS_FINE_LOCATION` 权限（仅影响图标下方的 SSID 文字显示，WiFi 开关不受影响）
- 移除仅限 `virgo-perf1` 机型的常驻「回桌面」通知服务（`HomeEntranceService`）及 `FOREGROUND_SERVICE` 权限
- 移除「打开设备管理器」设置项：一键锁屏功能自身的授权引导弹窗已覆盖该场景，入口冗余（设备管理员声明与锁屏流程保留，功能不受影响）

### 修复

- 修复存储权限授权后回调丢失导致「自定义图标」首次点击无响应的问题
- 修复硬件 BACK 键被拦截导致设置页无法用返回键退出的问题
- 修复管理模式翻页后空格子可能残留「删除/隐藏」按钮的问题

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
