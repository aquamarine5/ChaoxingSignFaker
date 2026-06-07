# Contributing Guide

> [!CAUTION]
> ChaoxingSignFaker（随地大小签）是一个开源应用，使用AGPLv3.0许可证发布。开源代码本源为让源代码对所有人开发，保持代码的开放性并欢迎任何人参与到项目的开发中来，但**不欢迎**任何形式的修改代码、名称等进行二次分发、换皮和商业化等行为。

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