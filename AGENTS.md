# 项目开发规范

- 使用 `runCatching` 处理异常，不使用 `try-catch`；协程取消异常应继续传播。
- 使用 `checkPredictable` 和 `requirePredictable`，不使用 Kotlin 标准库的 `check` 和 `require`。两个辅助函数位于 `ChaoxingResponseHelper.kt`，检查失败时抛出 `ChaoxingPredictableException`。
- 生成代码时不生成注释。

## 外部跳转入口（AI 辅助生成的 diff 总结）

分支 `feature/external-sign-entry`，纯导航，不代提交、不做静默签到、无白名单开关。

- `AndroidManifest.xml`：`MainActivity` 新增 intent-filter action `org.aquamarine5.brainspark.chaoxingsignfaker.action.OPEN_SIGN`（exported + DEFAULT category）。
- 新增 `ExternalSignEntry.kt`：
  - `Intent.parseExternalSignRequest()` 解析 extras：`classId`(Int)、`courseId`(Long)、`fid`(Int)、`activeId`(Long)、`signType`(String)、`courseName`(String，仅用于标题展示，可省略)。
  - `ExternalSignEntry.destinationOf()` 把 `signType`（gesture/location/password/photo/qrcode）映射为对应签到页 Destination。
- `MainActivity.kt`：`onCreate`/`onNewIntent` 捕获该 action 到 `pendingSignRequest`，会话就绪后在 `LaunchedEffect` 中处理：
  - 校验（`requirePredictable`）：跳转签到列表必传 `classId`、`courseId`、`fid`；跳转签到事件必传 `activeId` 且 `signType`（两者成对出现）；`fid` 不在当前账号 `fidList` 时由 `updateConfiguredFid` 内的 `requirePredictable` 拒绝。
  - `fid` 与当前 `configuredFid` 不同时先切换学校单位；未登录直接 toast 拦截。
  - 签到列表：`courseName` 缺省时用 `queryClassName(classId)` 补课程名，构造 `ChaoxingCourseEntity` 导航到课程签到活动列表页。
  - 签到事件：映射 Destination 直接导航，签到按钮与主观操作仍由用户在随地大小签页面内完成。
  - 所有失败路径走 `toastReport`，不静默。
- 新增 `ExternalCourseProvider.kt`（静默获取，不弹任何界面）：
  - exported ContentProvider，authority `org.aquamarine5.brainspark.chaoxingsignfaker.courses`，仅实现 `call("getCourses")`。
  - 复用当前登录态（`instance` 为空时从 DataStore 恢复会话，未登录/过期返回 error Bundle，不抛异常跨进程）。
  - 返回 Bundle：`json` = `{"fid":配置学校ID,"courses":[{"name","teacher","classId","courseId"}]}`，供课表类 App 按课程名匹配回填 ID；错误时返回 `error`。
  - 同名课程多门时，按各门最近一次签到活动的 startTime 取最大者（查询失败按无活动处理），只暴露近期有签到的那门。
  - 不含任何签到能力，不代提交。
