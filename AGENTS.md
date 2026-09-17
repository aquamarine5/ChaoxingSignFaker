# 项目开发规范

- 使用 `runCatching` 处理异常，不使用 `try-catch`；协程取消异常应继续传播。
- 使用 `checkPredictable` 和 `requirePredictable`，不使用 Kotlin 标准库的 `check` 和 `require`。两个辅助函数位于 `ChaoxingResponseHelper.kt`，检查失败时抛出 `ChaoxingPredictableException`。
- 生成代码时不生成注释。
