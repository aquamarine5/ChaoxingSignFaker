# 1.19.4-stable

## What's Changed

- v1.19.4: by [@aquamarine5](https://github.com/aquamarine5) in [\#238](https://github.com/aquamarine5/ChaoxingSignFaker/pull/238)

- 新增：签到时若提示"同一设备不允许重复签到"，会自动将该用户的设备码重置为随机设备码并重新签到，避免签到失败，感谢 @lzy24626 发现问题。

- 新增：在其他用户列表和选择签到用户页面，为设备码因重复签到已回退为随机设备码的用户显示警告图标和说明。

- 新增：对当前软件暂不支持的旧版本签到活动置灰显示，点击时提示"当前软件暂不支持此类型旧版本签到，请期待后续版本更新。"

- 新增：赞赏者列表中前三名分别使用金、银、铜色着色。

## Improvements

- 优化了人脸识别的健壮性，避免索引越界导致崩溃。

- 优化了相机拍照结果的投递逻辑，避免重复投递照片。

- 优化了设置页面，避免未登录时出现空指针崩溃。

- 优化了版本更新提示对话框，内容过长时可滚动查看。

- 优化了选择签到用户页面的标签点击状态判断，避免索引越界。

- 优化了选择签到用户页面，不再将登录用户自己计入标签用户列表。

- 升级了部分依赖库版本，包括 Kotlin、fastjson2、kotlinx.serialization、OkHttp、Stackbricks 等。

- 重构了网页下载页面，新增对 Cloudflare CDN 的支持。

**Full Changelog**: [1.19.3-stable...1.19.4-stable](https://github.com/aquamarine5/ChaoxingSignFaker/compare/1.19.3-stable...1.19.4-stable)

### Contributors

- [ ![@aquamarine5](https://avatars.githubusercontent.com/u/50990424?s=64&v=4)](https://github.com/aquamarine5)

aquamarine5
