# Jenkins Aliyun OSS Plugin - 构建指南

## 环境要求

- **JDK**: 21 或更高版本
- **Gradle**: 8.0+ (项目包含 Gradle Wrapper，无需单独安装)

## 快速开始

### 1. 构建并测试插件

```bash
./gradlew clean build
```

这将：
- 清理之前的构建
- 编译源代码
- 运行所有测试
- 生成 JAR 文件

### 2. 构建 HPI 插件包

```bash
./gradlew clean hpi
```

HPI 文件位置：`build/libs/jenkins-aliyun-oss-plugin-fork-<version>-hpi.jar`

这个文件可以直接安装到 Jenkins 中。

### 3. 查看插件信息

```bash
./gradlew pluginInfo
```

输出示例：
```
==========================================
Plugin: Aliyun OSS Plugin for Jenkins
==========================================
Group: org.jenkins-ci.plugins
Version: 1.0.0-M1
Jenkins Version: 2.440.3
Java Version: 21
==========================================
```

### 4. 生成发行包

```bash
./gradlew dist
```

发行包位置：`build/distributions/`

## 可用的 Gradle 任务

| 任务 | 描述 |
|------|------|
| `build` | 构建并测试插件 |
| `clean` | 清理构建目录 |
| `jar` | 构建 JAR 文件 |
| `hpi` | 构建 HPI 插件归档文件 |
| `test` | 运行测试 |
| `pluginInfo` | 显示插件信息 |
| `dist` | 构建分发包 |
| `dependencies` | 显示项目依赖 |
| `tasks` | 显示所有可用任务 |

## 常用构建命令

### 只编译不测试
```bash
./gradlew clean compileJava
```

### 运行特定测试
```bash
./gradlew test --tests "UtilsTest"
```

### 查看依赖树
```bash
./gradlew dependencies
```

### 刷新依赖
```bash
./gradlew --refresh-dependencies
```

## 构建输出

构建成功后，将在 `build/libs/` 目录下生成以下文件：

- `jenkins-aliyun-oss-plugin-fork-<version>.jar` - 主 JAR 文件
- `jenkins-aliyun-oss-plugin-fork-<version>-hpi.jar` - Jenkins 插件包
- `jenkins-aliyun-oss-plugin-fork-<version>-tests.jar` - 测试代码 JAR

## 安装到 Jenkins

### 方法 1: 通过 Web UI
1. 登录 Jenkins
2. 进入 **系统管理** → **插件管理**
3. 选择 **高级** 标签页
4. 在 **Upload Plugin** 部分上传 HPI 文件
5. 重启 Jenkins

### 方法 2: 手动安装
1. 将 HPI 文件复制到 Jenkins 插件目录：
   ```bash
   cp build/libs/jenkins-aliyun-oss-plugin-fork-<version>-hpi.jar $JENKINS_HOME/plugins/
   ```
2. 重启 Jenkins

## 本地测试运行

如果需要在本地的 Jenkins 中测试插件：

```bash
# 使用 Jenkins test harness
./gradlew test
```

或者使用 Maven 方式（如果配置了）：
```bash
mvn hpi:run
```

## 故障排除

### 问题：构建失败，提示找不到 Jenkins 核心依赖
**解决方案**: 确保可以访问 Jenkins 仓库
```bash
# 检查网络连接
curl -I https://repo.jenkins-ci.org/public/
```

### 问题：测试不执行
**解决方案**: 确认使用的是 JUnit 5
```bash
./gradlew test --info
```

### 问题：HPI 构建失败，提示重复文件
**解决方案**: 已配置 `duplicatesStrategy = EXCLUDE`，如果仍有问题，清理后重试
```bash
./gradlew clean hpi
```

## 版本管理

修改版本号在 `build.gradle.kts`:
```kotlin
version = "1.0.0-M1"  // 修改此处
```

## CI/CD 集成

项目包含 Jenkinsfile，可以在 Jenkins 中直接使用 Pipeline 构建。

## 其他资源

- [Jenkins 插件开发指南](https://www.jenkins.io/doc/developer/)
- [Jenkins 插件架构](https://www.jenkins.io/doc/developer/plugin-architecture/)
- [Gradle 用户指南](https://docs.gradle.org/current/userguide/userguide.html)
