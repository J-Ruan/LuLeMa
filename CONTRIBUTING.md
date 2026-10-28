# 参与开发

先阅读 README 和 docs/DEVELOPMENT.md。欢迎通过 Issue 报告问题，再提交小范围、易审核的 PR。

- 使用 Kotlin、Compose 和官方 AndroidX 库。保持黑白简洁的设计。
- 不加入网络权限、广告、用户追踪、频率排名或羞辱式文案。
- 测试数据必须虚构。不要在 Issue、截图或日志中暴露真实记录。
- 提交前运行 `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
- 数据层或 UI 改动需在设备上运行 `./gradlew :app:connectedDebugAndroidTest`。
- 修改数据库结构须递增版本、添加迁移和迁移测试；不能使用破坏性迁移。
- 更新功能或版本时同步 README、CHANGELOG 和工作流版本，发布遵循 docs/RELEASING.md。
- 严禁提交密钥、Keystore、个人备份和本地路径。贡献遵循 MIT 许可。
