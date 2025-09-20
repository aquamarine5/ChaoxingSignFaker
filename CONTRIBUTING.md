# Contributing Guide

## 配置 [Stackbricks](https://github.com/aquamarine5/Stackbricks) 服务

- ChaoxingSignFaker（随地大小签）使用[Stackbricks](https://github.com/aquamarine5/Stackbricks)作为更新服务框架，其被托管在Github Packages上
- Github Package源并不会在默认的项目内配置，因此需要手动设置

> [!NOTE]
> 建议遵循[Github官方文档](https://docs.github.com/zh/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)进行部署。

- 在项目根目录的`setting.gradle`文件中添加如下内容：
```groovy
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/aquamarine5/Stackbricks")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GPR_USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("GPR_TOKEN")
            }
            content {
                includeModule("org.aquamarine5.brainspark","stackbricks")
            }
        }
    }
}
```
> [!WARNING]
> 请确保`gpr.user`和`gpr.key`的值已经在`gradle.properties`配置，或通过环境变量配置。

## 项目结构

- ChaoxingSignFaker（随地大小签）使用 [Jetpack Navigation](https://developer.android.google.cn/jetpack/androidx/releases/navigation?hl=zh-cn) 进行导航
- `ChaoxingHttpClient`类包含了学习通账号的功能。
- `ChaoxingSigner`是所有签到方法的基类，包含了预签到和验证码等方法。
- `ChaoxingPhotoSigner`包括拍照签到和点击签到（因为他们的`type`值相等）。
