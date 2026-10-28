# 开发与架构

## 构建环境

兼容组合固定为 JDK 21（字节码目标 17）、Gradle 8.11.1、AGP 8.9.2、Kotlin / Compose Compiler 2.1.20、Compose BOM 2025.04.01、Room 2.7.1、KSP 2.1.20-1.0.32。SDK 35 / Build Tools 35.0.0，最低 API 26。不要独立升级 KSP 或 Kotlin 导致版本不匹配。

用 Android Studio 打开仓库根目录，等待 Gradle Sync。SDK 路径配置在本机 `local.properties`，不提交。命令行可用 `ANDROID_HOME` 指向 SDK。

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest # 需要已启动的模拟器 / USB 设备
```

Gradle Wrapper 固定官方分发包 SHA-256。保持 TLS 与 SDK 包校验开启。云环境如使用出站代理，Java 也需显式配置代理和受信任 CA；不能通过关闭 TLS 校验解决下载问题。SDK 协议应由使用者确认。

## 代码结构

- `data/Models.kt`：Room 实体、DAO、Repository；记录以本地日历日期的 epoch day 存储，不因时区转换而移动。可选时间是当天分钟数。
- `domain/Analysis.kt`：无 Android 依赖的日期、统计、月历和文案 / 成就定义。
- `ArchiveViewModel.kt`：StateFlow、生命周期作用域、用户操作和后台备份任务。
- `ui/`：四个主要页面、记录 Bottom Sheet、成就页、主题与图表。
- `backup/BackupCodec.kt`：版本化 JSON、输入验证、密码加密。
- `notification/`：WorkManager 周期提醒与中性通知。

记录和设置、成就共同存于 Room。此处特意将设置存于同一个数据库，而非另用 DataStore，以便覆盖恢复和清除操作在单一事务中完成，不会出现记录恢复了、设置只恢复一半的问题。UI 使用 Flow 实时更新。

Room schema 导出到 `app/schemas`。后续变更必须添加迁移与测试，禁止破坏性迁移。数据库不设置 fallbackToDestructiveMigration。

## 输入和统计约定

- 同一天允许多条记录，每条有 UUID；补录范围 1900-01-01 至本机今天。
- 时间可省略；时长 0–1440 分钟，0 表示未填写；心情至多 20 字、备注至多 1000 字。
- 次数按条统计，不去重日期；总时长用 Long；平均包含未填写时长的 0，界面会说明。
- 最近 7 / 30 天含今天与无数据日期。月度按日历月，年度按日历年。闰年由 java.time 处理。
- 日期是用户本地日期，时间不是 UTC 时间戳。创建 / 修改时间与成就时间则是 Unix 毫秒。
- 图表生成在 Default 线程；备份编解码、文件操作在 IO 线程；Room 使用自己的后台执行器。

## 生命周期与交互

ViewModel 保留数据流，导航及编辑文本用 rememberSaveable 恢复；密码只保存在内存，不写 SavedState、磁盘或日志。系统杀进程期间丢失的待处理备份凭据会使操作取消，不会降级为明文导出。

系统日期每 30 秒刷新；Compose 动画遵循系统动画时长缩放（关闭动画时不播放）。大屏页面限制最大阅读宽度，表单可滚动且支持输入法避让。

提醒默认关闭，间隔 7–90 天。WorkManager 是近似周期调度，系统省电策略可能延迟。更改时区或夏令时后，可重新保存提醒时间重新调度。应用内禁用提醒会取消任务和已显示通知；用户仍可在系统设置中关闭通知。

## 测试

JVM 测试覆盖验证、空档案、统计、闰年、跨年、星期起始、跨时区、明文 / 加密备份、错误密码、篡改、坏文件、重复 ID、超限和深度限制。

设备测试覆盖 Room 新增 / 编辑 / 删除、同日多条、补录、关闭重开数据库、设置持久化、较新记录合并、覆盖导入、无效导入不变更数据，以及 Compose 新增和四页导航。

CI 的独立设备测试 job 在 API 30 模拟器上运行。请额外在真机验证 OEM 通知策略、字体缩放、Android 13+ 权限、SAF 文件提供器和 Android 8 兼容性。不能将编译成功等同于这些设备验证全部通过。
