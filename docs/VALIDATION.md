# v0.2.0 验证与交付

日期：2026-10-29（Asia/Shanghai）。全部测试数据为人工构造。历史 v0.1.0 的结果归档于 [旧版验证报告](history/VALIDATION-v0.1.0.md)，不能混作本次结果。

## 本地验证

已执行：

```bash
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
```

- JVM 单元测试 42 项通过，0 失败、0 跳过。
- Lint 0 错误、16 警告：14 项现有依赖的新版本提示，以及标准 v26 自适应图标目录、中文完整矢量轮廓两项原有警告。保持当前兼容依赖，未关闭检查或删除断言。
- Debug、AndroidTest 和 R8 Release 构建成功。未向本地提供正式签名，Release 输出 unsigned，仅作构建验证，不能作为可安装的正式版。
- 工作流 YAML、发布冒烟脚本 Python 语法与 git diff --check 通过。

API 30 / Android 11、320×640、160 dpi 的专用模拟器中，真实数据库迁移、Room CRUD / 新字段持久化 / 合并覆盖回滚、成就补算、实际加密文件导出预览恢复及篡改拒绝、WorkManager 任务 UUID 保持等 6 项后端设备测试已经通过。

完整 Compose 流程当前仍在验证。此实例没有 /dev/kvm，TCG 启动时出现 System UI、电话、蓝牙和 system 进程的无响应对话框；处理启动对话框后，交互测试仍遇到输入派发超时。已添加仅含阶段名称的测试日志，保留全部断言，并将 R8 构建与交互分开运行。不得将不完整设备轮次描述为通过，最终以硬件加速 CI 的真实结果补充本报告。

## 实现与验证范围

- 单元测试覆盖单日 2–6 次同时解锁、UUID 幂等、历史连续日期 / 中断 / 跨年闰日、时长点评及 0/3/60 边界、解锁后不撤销。
- 新字段覆盖非负整数 / 上限、小数 / NaN / Infinity / 精度 / 上限、地点长度、空值与真实 0；合计使用 Long 和 BigDecimal。
- 报告覆盖自然周起始、跨年周、闰月 / 闰年、截止今天、空数据、有效时长平均值、地点分组与按本地日期归属的新勋章。
- 备份覆盖真实 v1 缺失字段恢复为空、新格式明文 / AES-GCM 往返、密码 / 篡改 / 超限 / 深度 / 重复 ID / 缺身份字段及异常未来时间戳。
- MigrationTestHelper 用已提交的 schema 1 创建旧数据库，插入旧记录、设置和解锁时间，显式迁移至 2，验证三列为 NULL、原数据及升级后 CRUD。应用注册同一迁移，不使用破坏性回退。
- Compose 流程保留输入、重建恢复、详情点评、编辑、新字段回显、报告切换和刷新、独立勋章墙、五页导航、深色重建和删除检查；当前不能宣称完整通过。

## 发布门槛与实际状态

本次代码版本为 0.2.0 / versionCode 2，包名 com.lulema.app，minSdk 26、targetSdk 35。当前只确认源码实现及上述本地结果；v0.2.0 尚未发布。

本地 GitHub 凭据可以读写仓库，但列出 Actions Secrets 的接口返回 403。工作流新增 signing-readiness，仅在仓库执行器中检查 KEYSTORE_BASE64、KEYSTORE_PASSWORD、KEY_ALIAS、KEY_PASSWORD 是否齐全，输出只包含缺失名称，不输出秘密值。不能根据环境未绑定本地密钥而推断仓库 Secrets 缺失。

仅在 main 的测试及实际正式签名候选包的签名、安装、启动和关键流程通过后创建 v0.2.0；缺签名则仅允许明确标记的 v0.2.0-debug.1 预发布。发布后必须重新下载真实附件核对 SHA-256 和签名，再记录链接与校验值。

## 兼容性与未覆盖范围

- 旧记录以 duration=0 表示未填写，平均值和时长曲线统一忽略这些值；旧字段及九枚成就 key 不变，新列可空。
- 旧 v1 明文 / 加密备份可导入；新导出格式 v2 不能给旧 v0.1.0 导入，升级前请保留旧备份。
- 开发签名可能随机器改变，公开 v0.1.0 Debug 不承诺直接覆盖升级；正式签名与 Debug 不同，切换前先备份，必要时卸载旧包后恢复。
- 尚未做 Android 8 真机、Android 13+ 通知授权、厂商省电策略及各 SAF 云盘提供器的完整回归；实际设备与 Release 验证将按执行结果补充，不以编译成功替代。
