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
- `ui/`：首页、日历、统计、成就、我的五个一级页面，记录表单 / 详情 Bottom Sheet、主题、勋章 Canvas 与图表。
- `domain/AchievementEngine.kt`：稳定 key、单日阈值、日期连续性、幂等且不撤销的持久解锁。
- `domain/DurationEvaluator.kt`：独立的单次时长点评；`EntryInput.kt`：选填数字的严格解析。
- `domain/ReportAnalyzer.kt`：自然周 / 月 / 年、本地日期边界、地点分布与新勋章；`ReminderConfiguration.kt`：仅提醒配置变化需要重排。
- `backup/BackupCodec.kt`：版本化 JSON、输入验证、密码加密。
- `notification/`：WorkManager 周期提醒与中性通知。

记录和设置、成就共同存于 Room。此处特意将设置存于同一个数据库，而非另用 DataStore，以便覆盖恢复和清除操作在单一事务中完成，不会出现记录恢复了、设置只恢复一半的问题。UI 使用 Flow 实时更新。

Room schema 1、2 均保留在 `app/schemas`。`ArchiveDatabase.MIGRATION_1_2` 仅以 ALTER TABLE 增加可空的 handCount INTEGER、volumeMl REAL、location TEXT；应用构建数据库时显式注册。旧记录三个字段为 NULL，profile 原文不变。MigrationTestHelper 使用已提交的旧 schema 创建真实 v1 数据库并验证迁移、旧设置 / 成就和升级后的 CRUD；不使用破坏性迁移。

## 输入和统计约定

- 同一天允许多条记录，每条有 UUID；补录范围 1900-01-01 至本机今天。
- 时间可省略；时长 0–1440 分钟，0 表示未填写；心情至多 20 字、备注至多 1000 字。
- 记录次数按条统计，不去重日期；总时长用 Long；平均和时长趋势只含 duration > 0 的已填值，0 继续表示旧版的未填写。界面展示未填写数量。
- handCount 为 Int?，非负整数，合计使用 Long；volumeMl 为 Double?，0–1000 mL、最多三位小数，使用 BigDecimal.valueOf 累加，避免 0.1 + 0.2 的二进制显示误差；location 为 String?，手动填写、最多 100 字。空数字 / 地点规范为 null，真实 0 保留为 0。
- 记录创建 / 修改、成就解锁和备份导出时间必须为正 Unix 毫秒且不晚于本机当前时间五分钟；容忍短暂时钟偏差，拒绝极大未来值。修改时间不能早于创建时间。导入被拒时应先核对设备时钟，不静默修正备份。
- 周报按 mondayFirst 使用自然周；月报 / 年报按自然月 / 年；当前周期截到今天，并标记进行中。连续天数显示周期内最长连续自然日；新勋章按本地解锁日期归属。报告从数据库 Flow 派生，不额外持久化统计表。
- Repository 在保存、删除、导入 / 合并 / 覆盖以及启动检查时处理成就；现有 UUID Upsert 防止编辑重复计数。已解锁时间保持不变，删除不撤销；覆盖按导入设置恢复后对数据补算。合并保留本机设置和原有勋章，再检查合并后的记录。
- 最近 7 / 30 天含今天与无数据日期。月度按日历月，年度按日历年。闰年由 java.time 处理。
- 日期是用户本地日期，时间不是 UTC 时间戳。创建 / 修改时间与成就时间则是 Unix 毫秒。
- 图表生成在 Default 线程；备份编解码、文件操作在 IO 线程；Room 使用自己的后台执行器。

## 生命周期与交互

ViewModel 保留数据流，导航及编辑文本用 rememberSaveable 恢复；密码只保存在内存，不写 SavedState、磁盘或日志。系统杀进程期间丢失的待处理备份凭据会使操作取消，不会降级为明文导出。

系统日期每 30 秒刷新；Compose 动画遵循系统动画时长缩放（关闭动画时不播放）。大屏页面限制最大阅读宽度，表单可滚动且支持输入法避让。

提醒默认关闭，间隔 7–90 天。WorkManager 是近似周期调度，系统省电策略可能延迟。仅启用状态、时间、间隔发生变化时重排，昵称、主题和星期起始日不会改变任务；启动检查使用 KEEP。更改时区或夏令时后，可调整提醒时间或关闭后重新开启。应用内禁用提醒会取消任务和已显示通知；用户仍可在系统设置中关闭通知。

## 测试

JVM 测试覆盖验证、空档案、统计、闰年、跨年、星期起始、跨时区、明文 / 加密备份、错误密码、篡改、坏文件、重复 ID、超限和深度限制。

设备测试会清空或卸载测试 App，请只使用专用模拟器或已备份的测试设备，不能在持有真实档案的手机上直接运行。

设备测试覆盖 Room 新增 / 编辑 / 删除、同日多条、补录、关闭重开数据库、新字段和设置持久化、较新记录合并、覆盖导入、无效导入不变更数据、真实 v1→v2 迁移、加密文件导出 / 预览 / 恢复 / 篡改拒绝，以及 Compose 三个新字段、Activity 重建后表单恢复、详情点评、五页导航、报告刷新与深色主题重建后保留。ReminderTest 还比较实际 WorkManager 任务 UUID，验证修改昵称 / 主题 / 星期起始不重排任务。

CI 的独立设备测试 job 在 API 30 模拟器上运行。请额外在真机验证 OEM 通知策略、字体缩放、Android 13+ 权限、SAF 文件提供器和 Android 8 兼容性。不能将编译成功等同于这些设备验证全部通过。
