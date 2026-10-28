# 构建、签名与发布

## 下载和更新

仓库的 Actions → Android → 成功运行 → Artifacts → `LuLeMa-v0.1.0-debug`。登录 GitHub 下载并解压 ZIP，取出 APK。正式版本在 Releases 下载 `LuLeMa-v0.1.0.apk` 与 `SHA256SUMS`。只有实际存在的 Release 才是已发布版本。

Debug 使用开发签名，适合测试；不同开发机器生成的 Debug 密钥可能不同，不能保证跨机器直接升级。正式签名与 Debug 签名不同，首次切换通常需要先备份、卸载再安装。升级前始终主动备份。

## 维护代码

1. Fork / 克隆仓库，在 Android Studio 修改 Kotlin 和资源文件。
2. 运行单元测试、Lint、Debug 构建及设备测试。
3. 新分支提交，创建 PR；合并到 main 后自动构建。小改动也可以在 GitHub 网页编辑后提交，等待 Actions 验证。
4. Actions 页的 Run workflow 可手动触发。没有安装包 Artifact 时先查看失败步骤，不能跳过测试假装成功。

## 正式签名配置

由维护者在可信机器生成并离线备份正式 Keystore；不要在公共仓库创建或上传私钥。

```bash
keytool -genkeypair -v -keystore lulema-release.jks -alias lulema -keyalg RSA -keysize 4096 -validity 10000
```

命令会交互式询问密码；不要把密码写进 shell 历史。GitHub 仓库 Settings → Secrets and variables → Actions 添加：

- `KEYSTORE_BASE64`：Keystore 的 Base64 编码，需在本地安全转换。
- `KEYSTORE_PASSWORD`：Keystore 密码。
- `KEY_ALIAS`：签名别名。
- `KEY_PASSWORD`：密钥密码。

这四项只应作为 GitHub Actions Secrets，不是公开变量。不要将值发送到聊天、Issue 或日志。工作流只在拥有全部四项时构建签名包，临时密钥会在退出和结束步骤中清理。缺少任何一项会在 Summary 明确说明跳过正式发布，同时保留 Debug Artifact。

本地签名构建支持 `KEYSTORE_FILE`（绝对路径）及后三个密码 / 别名环境变量。环境应从安全存储注入。`./gradlew :app:assembleRelease` 在未配置密钥时只生成 unsigned APK，不能安装或冒充正式版本。

## 新版本

同步修改 `app/build.gradle.kts` 的 versionCode（递增）、versionName、工作流 VERSION、CHANGELOG 和文档。

```bash
git tag -a v0.1.0 -m 'LuLeMa v0.1.0'
git push origin v0.1.0
```

Tag 必须与 APK 版本一致。工作流将等待 build 与 device-tests 成功，再构建、验证签名、生成 SHA-256、保留 Release Artifact 并调用 GitHub CLI 发布。内置 `GITHUB_TOKEN` 需要 contents:write，仓库 / 组织策略也须允许。未实际推送 Tag 就不会自动发布。

如果 Release API 发布失败，签名包仍保存在 Release Artifact。维护者检查权限和错误后可用 GitHub Releases 手动选择已验证 Tag 并上传已签名 APK、校验值和说明；不要上传 unsigned APK。不要给 PR 提供发布凭据。

## 建议 Topics

`android`, `kotlin`, `jetpack-compose`, `offline-first`, `privacy`, `habit-tracker`, `room`。在仓库 About 中维护；设置 Topics 需要仓库管理权限。

## Debug 预发布

当前 [v0.1.0-debug.1](https://github.com/J-Ruan/LuLeMa/releases/tag/v0.1.0-debug.1) 已发布。后续测试版使用 `v0.1.0-debug.2` 等新标签。`debug-prerelease` job 会等待 build 与 device-tests 成功，下载本次构建已核验签名的 Artifact，检查 SHA256SUMS，再以 Pre-release 发布 Debug APK。无需正式签名 Secrets，也不会执行正式签名发布 job。

```bash
git tag -a v0.1.0-debug.2 -m 'LuLeMa v0.1.0 Debug prerelease 2'
git push origin v0.1.0-debug.2
```

后续测试版递增 `debug.2` 等后缀，不覆盖旧标签或安装包。Debug 版本不承诺跨构建签名一致，升级前必须备份。
