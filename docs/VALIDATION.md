# v0.1.0 验证报告

日期：2026-10-28。所有测试数据均为人工构造，截图来自真实 Android 模拟器运行。

## 最终本地验收

实际执行并成功退出（`BUILD SUCCESSFUL in 4m 11s`）：

```bash
./gradlew --no-daemon \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  :app:assembleRelease :app:connectedDebugAndroidTest \
  -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

| 检查 | 结果 |
|---|---|
| Kotlin JVM 单元测试 | 19 项通过，0 失败、0 跳过 |
| API 30 设备测试 | 3 项通过，0 失败、0 跳过 |
| Android Lint | 0 错误，2 项警告 |
| Debug APK | 实际构建、签名验证、安装、启动和 UI 流程通过 |
| Release / R8 构建 | 成功；未提供正式签名，所以输出 unsigned，不用于安装或发布 |
| 可复用安装脚本 | 在当前实例实际执行成功，冻结 Wrapper 校验值和工具校验值 |

单元测试包含输入验证、空档案、同日多条、月度 / 年度统计、时长、闰年、星期起始、日期跨时区、明文 / 加密备份、错误密码、篡改、损坏 / 超大 / 过深 JSON、版本与必填身份字段缺失、重复 ID、提醒频率和成就幂等性。

设备测试包括：

1. Compose 录入时长和虚构备注，重建 Activity 后验证表单仍在，保存记录，确认首页更新，切换日历、统计和我的；切换深色模式并重建 Activity 验证主题保留；保存四页及深色模式共五张真实截图。
2. Room 新增、编辑、删除、同日多条、历史日期，关闭并重开数据库验证记录、主题、昵称与成就持久化。
3. 按 ID 合并、旧版本不覆盖新记录、无效覆盖不改原数据、完整覆盖时关闭提醒，以及清空设置和记录。

Lint 警告为 `ObsoleteSdkInt`（自适应图标使用标准 v26 目录）和 `VectorPath`（保留完整中文笔画）。保留真实“撸”字轮廓与兼容目录，没有通过关闭检查来隐藏警告。

## APK 核验

- 文件名：`LuLeMa-v0.1.0-debug.apk`
- 构建类型：Debug，开发签名，APK Signature Scheme v2 验证通过。
- 大小：18,230,964 字节，约 17.39 MiB。
- applicationId：`com.lulema.app`
- versionName / versionCode：`0.1.0` / `1`
- minSdk / targetSdk：`26` / `35`
- App 标签：`撸了吗`
- 无 INTERNET 权限，无广告 / 追踪 SDK。
- SHA-256：`203c267b08a06253d613572cb38c65ae10bce1523d0aab37034e0967d54e4cb0`

以上 SHA-256 仅对应本任务本地交付的 APK。CI 的 Debug 签名密钥可能不同，请使用 Artifact 内附的 SHA256SUMS 核对该次下载。

本地校验命令：`apksigner verify --verbose`、`aapt dump badging`、`sha256sum`。实际安装成功；四页测试在禁用模拟器 Wi-Fi / 移动数据时执行。

## 实际系统文件选择器检查

在仅含虚构数据的 API 30 测试模拟器中，通过真实 DocumentsUI 选择 Downloads 位置，成功导出 820 字节加密备份。文件只含 encryptedVersion、salt、iv、ciphertext，不含明文 records。额外使用标准 AES-GCM 实现独立认证解密，得到 1 条记录、15 分钟。

随后通过 App 的系统文件选择器选回该文件，输入虚构测试密码，实际预览显示 1 条记录、15 分钟；选择合并后，统计显示 2 条不同 UUID 的记录，符合保留本机与历史备份的合并语义。此检查与 JUnit 的 19 + 3 项计数分开记录，没有把手动检查冒充自动化用例。

## 历史失败与修复

- 首轮编译有通知资源 `R` 的导入歧义，添加显式导入后重跑通过。
- 首轮软件仿真尚未稳定，出现系统 UI ANR 和输入派发超时，整轮设备测试中断，未将未执行的用例计为通过。关闭模拟器网络后台活动、等待系统稳定后，先分别通过数据库 / UI 测试，再整轮通过 3 项测试。
- 截图等待一度误匹配首页语录，超时后改成精确匹配短暂 Snackbar；最终整轮截图和测试通过。没有删除断言或测试。

## GitHub 与自动构建

公开仓库：[J-Ruan/LuLeMa](https://github.com/J-Ruan/LuLeMa)。实际 HTTPS 推送已验证；仓库页面元数据显示 Public。

[自动构建记录](https://github.com/J-Ruan/LuLeMa/actions)。远端 `build` job 已实际通过单元测试、Lint、Debug / AndroidTest 构建、签名核验和 Artifact 上传，例如 [运行 #3](https://github.com/J-Ruan/LuLeMa/actions/runs/40662260498) 的 build job。后续取消针对卡住的设备 job，不代表 build 未执行。

首轮远端第三方 SDK 安装失败后，改用 Ubuntu 24.04 预装 SDK 和官方当前维护 Actions。远端设备 job 曾卡在模拟器启动，后续版本明确 SDK / adb 路径、加入有界等待和日志诊断。已定位并修复 AVD 创建与启动目录不一致的问题：[运行 #7](https://github.com/J-Ruan/LuLeMa/actions/runs/40666922782) 的 build 和 device-tests 两个 job 均实际成功，完整流水线用时 4m 21s。[运行 #8](https://github.com/J-Ruan/LuLeMa/actions/runs/40668601917) 也已完整通过，包含最终深色系统栏及主题重建测试。

## 尚未验证 / 发布条件

- 未在真实手机或 Android 8 / Android 13+ 上执行完整回归；特别是通知运行时授权、OEM 省电策略、SAF 各文件提供器与大字体需要真机检查。
- API 30 的系统 DocumentsUI 加密导出、密码导入预览和合并已实际验证；其他 OEM / 云盘文件提供器尚未回归。
- 没有正式 Keystore 和四项签名 Secrets，未生成正式签名 Release APK，未创建 GitHub Release。
- GitHub API 在当前环境返回 Forbidden；Git 推送及公开网页读取可用，未设置仓库 Topics。维护者可按发布文档手动设置。
- 已保存云环境 `install_script` / `start_skill` 草稿。保存不代表发布快照或新任务恢复已验证；请在环境设置中检查并保存后发布。
