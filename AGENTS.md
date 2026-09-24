# 项目开发规范

- 使用 `runCatching` 处理异常，不使用 `try-catch`；协程取消异常应继续传播。
- 使用 `checkPredictable` 和 `requirePredictable`，不使用 Kotlin 标准库的 `check` 和 `require`。两个辅助函数位于 `ChaoxingResponseHelper.kt`，检查失败时抛出 `ChaoxingPredictableException`。
- 生成代码时不生成注释。
- 不要使用`SharedPreference`。
- 程序的单元测试为空，请勿进行测试。
- AI在尝试构建项目时，请勿将构建的输出文件放在项目目录里面，即使是.gitignore目录也不行，请放在%TEMP%或其他位置。
- 布尔型变量命名应以 `is` 开头。