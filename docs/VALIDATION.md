# v0.2.0 验证与交付

日期：2026-10-29（Asia/Shanghai）。所有测试仅使用人工构造的数据。

## 实际交付

- [GitHub 预发布](https://github.com/J-Ruan/LuLeMa/releases/tag/v0.2.0-debug.2) · [APK 直接下载](https://github.com/J-Ruan/LuLeMa/releases/download/v0.2.0-debug.2/LuLeMa-v0.2.0-debug.apk) · [校验文件](https://github.com/J-Ruan/LuLeMa/releases/download/v0.2.0-debug.2/SHA256SUMS)。
- 这是 **Debug 开发签名预发布**，不是正式签名稳定版。
- 文件 LuLeMa-v0.2.0-debug.apk，18,275,669 字节；SHA-256：`8c3530c7d445dd0749711540633b70098eab08237cbe7065405c683279d01c38`。
- 包名 com.lulema.app，versionName 0.2.0，versionCode 2，minSdk 26、targetSdk 35。
- 已从公开、无需登录的 URL 重新下载，逐项核对附件 SHA256SUMS、APK Signature Scheme v2、包名、版本和权限。不存在 INTERNET、精确 / 粗略 / 后台定位权限。

## 真实检查结果

| 检查 | 结果 |
|---|---|
| JVM 单元测试 | 42 项通过，0 失败、0 跳过 |
| API 30 / Android 11 设备测试 | 7 项通过，0 失败、0 跳过 |
| Lint | 0 错误；本地 17、CI 18 警告：15 项依赖更新、原有 v26 图标及矢量路径；CI 另提示保持 targetSdk 35 未使用最新目标 API |
| Debug / AndroidTest 构建 | 成功，开发签名有效；公开附件已实际安装，并核对首页五个入口及初始 0 次记录 |
| R8 Release 构建与安装 | 优化构建成功，专用模拟器以仅供测试的临时签名安装、启动、新增、详情点评、五页导航和报告切换成功 |
| 小屏与大字体 | 本地 320×640、160dpi、1.5 倍字体的 R8 全流程成功；日历日期单行已截图人工核对，完成后恢复字体设置 |
| 正式签名稳定版 | 未发布，四项签名 Secrets 均缺失；没有把 unsigned 或测试签名 APK 冒充正式版 |

本地实际执行：

```bash
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
./gradlew --no-daemon :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

本地缺 /dev/kvm，TCG 交互轮次受启动系统 ANR 和输入派发超时影响，不计为完整通过；完整 7 项结果来自硬件加速 CI。代码中未删除断言或跳过迁移。Espresso 仅用于 AndroidTest 的键盘操作，不进入应用 APK。

## 可核对的远端记录

- [最终 main 验证 40875176118](https://github.com/J-Ruan/LuLeMa/actions/runs/40875176118)：build、7 项设备测试、R8 安装冒烟成功。
- [实际发布流水线 40876438177](https://github.com/J-Ruan/LuLeMa/actions/runs/40876438177)：标签门禁成功后 debug-prerelease 上传实际附件。正式 release job 按缺签名的真实状态跳过。
- [前一轮完整验证 40870347476](https://github.com/J-Ruan/LuLeMa/actions/runs/40870347476) 已核对真实 JUnit XML：7 项、0 失败 / 错误 / 跳过，31.446 秒。
- 核心实现提交 81b4ac5；完整灰阶主题与独立 R8 测试签名 6668354；大字体与报告断言 297ceca；键盘测试与显式依赖 6bfe537 / 3693083。实际发布 Tag 指向 3693083，后续 README / 截图 / 本报告同步在 main。

## 业务与迁移验证

- 原九枚成就 key 保留，新增八枚；单日 2–6 次逐边界、同 UUID 幂等、历史补录、连续日期 / 中断 / 跨月跨年闰日、0 / 3 / 60 分钟边界均测试。删除和编辑不撤销既有解锁时间，导入后在业务事务中检查。
- 手部运动次数为非负 Int?；出货量为 0–1000 mL、最多三位小数的 Double?，拒绝 NaN / Infinity；地点手填最多 100 字。未填为 null，真实 0 保留。手部次数合计用 Long，出货量用 BigDecimal，记录次数另行统计。
- 周 / 月 / 年报告按本地自然日期，星期起始遵循 mondayFirst；当前周期截止今天。平均值和时长曲线仅含 duration > 0 的已填值；报告展示未填写数量。界面实际断言：两条记录中只有一次填写 61 分钟时，平均仍为 61.0 分钟。
- Room schema 1、2 均保留，MIGRATION_1_2 用 ALTER TABLE 增加三个可空列并在应用注册。MigrationTestHelper 创建真实旧库、插入旧记录 / 设置 / 成就时间，升级后验证三列为 NULL、原数据完整及 CRUD；没有破坏性迁移。
- 新导出格式 v2、兼容 v1 明文和加密；保留 PBKDF2 / AES-GCM、AAD、大小 / 深度限制、导入预览、按较新 UUID 合并、事务覆盖与回滚。三个新字段与 null / 0 的往返、重复 ID、坏密码 / 篡改 / 超限 / 异常未来时间戳均覆盖。
- 创建 / 修改 / 解锁 / 导出时间必须为正且最多容忍本机五分钟偏差，修改不得早于创建；不静默改写异常文件。
- 实际 WorkManager UUID 测试验证昵称、主题、星期起始不重排，提醒配置改变才重排，关闭会取消；启动使用 KEEP 检查。
- Compose 真实录入三个字段、Activity 重建恢复、详情与编辑、报告刷新、独立勋章墙、五页导航、深色重建、删除均通过。文件导出 / 预览 / 恢复设备测试通过真实 ContentResolver 文件 URI 执行，篡改不改数据库。

## 签名条件与剩余维护操作

仓库执行器的 signing-readiness 已实际确认以下四项均缺失：KEYSTORE_BASE64、KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD。仅检查存在性，未输出秘密值。维护者需生成并长期离线保管同一份正式 Keystore，将四项配置为 GitHub Actions Secrets，先通过 main 的正式签名候选 APK 验证，再创建 v0.2.0 稳定 Tag、完成上传和下载校验。详见 [发布文档](RELEASING.md)。

Debug 与正式签名不同，开发签名也可能随执行器变化；升级前先导出并确认备份，签名冲突时再卸载、安装和恢复。v2 备份不能给旧 v0.1.0 导入，加密密码无法找回。

尚未覆盖 Android 8 真机、Android 13+ 通知授权、不同厂商省电策略及各 SAF 云盘文件提供器的完整手动回归。未将这些项目描述为通过。

## 失败记录与修正

- 本地首次增量编译有跨文件引用问题，清理生成文件后完整构建通过；本地软件仿真出现启动系统 ANR 及输入派发超时，不计作通过。公开 APK 安装成功，首次本地 am start -W 等待超时后首页实际加载；随后返回 Status: ok 并核对五页入口 / 初始空档案，不将该软件仿真的冷启动性能计为通过。
- 40865042327：7 项设备测试通过，但 R8 冒烟假定默认开发私钥存在而失败；改为独立生成、清理仅供测试的临时密钥。40866586982 随后完整成功。
- v0.2.0-debug.1 / 40871411137：展开选填字段时自动化操作遇到键盘布局时序，发布门禁阻止上传。保留失败 Tag，没有 Release。修正为先关闭键盘、等待字段；补齐显式测试依赖后编译和完整检查重新通过。3693083 保留全部断言。

旧版结果独立归档于 [v0.1.0 验证报告](history/VALIDATION-v0.1.0.md)。[本次实际文件清单](V0.2.0_FILES.md)。
