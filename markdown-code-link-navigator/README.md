# Markdown Code Link Navigator

一个面向 Android Studio / IntelliJ Platform 的轻量插件，用来在 Markdown 文档里直接跳转到本地代码文件。

## 功能概览

- 在 Markdown 编辑器中支持 `Ctrl+Click` 和 `Ctrl+B`
- 在 JetBrains 原生 Markdown 预览面板中支持点击本地代码链接
- 支持相对路径和绝对路径
- 支持跳到指定行：`#L42`
- 支持跳到并选中行范围：`#L42-L60`
- 支持跳到指定行列：`:42:5`

## 链接格式

支持这些格式：

```md
[MainActivity](app/src/main/java/com/example/MainActivity.kt#L42)
[MainActivity](app/src/main/java/com/example/MainActivity.kt#L42-L60)
[MainActivity](app/src/main/java/com/example/MainActivity.kt:42:5)
[Absolute Path](C:/project/ap p/src/main/java/com/example/MainActivity.kt#L42)
```

README 本身也用了这个格式，下面这些链接都可以直接跳到源码：

- [插件注册入口](src/main/resources/META-INF/plugin.xml#L1-L20)
- [编辑器 Ctrl+Click 实现](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt#L11-L56)
- [编辑器 Ctrl+B 实现](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt#L7-L35)
- [共享解析与导航核心](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L21-L293)
- [预览模式链接打开实现](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L9-L37)

## 使用方法

1. 在 Android Studio 或 IntelliJ IDEA 中打开一个 `.md` 文件。
2. 写入本地代码链接，例如：

```md
[共享解析核心](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L28-L107)
[预览模式入口](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L15-L23)
[插件注册](src/main/resources/META-INF/plugin.xml#L9-L18)
```

3. 在编辑器里使用 `Ctrl+Click` 或 `Ctrl+B`。
4. 在 Markdown 预览面板里直接点击同样的链接。

## 路径解析规则

- 相对路径先相对于当前 Markdown 文件所在目录解析
- 如果当前目录找不到，再回退到项目根目录解析
- 绝对路径直接定位
- `file://` 链接也会被识别
- 行号和列号按 Markdown 中常见的 1-based 写法输入，插件内部会转换成 IDE 使用的 0-based 坐标

核心实现位置：

- [路径与链接解析](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L28-L145)
- [最终打开文件并处理选区](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L220-L258)

## 实现说明

### 1. 插件如何接入 IDE

插件在 [plugin.xml](src/main/resources/META-INF/plugin.xml#L1-L20) 里注册了三条入口：

- [Markdown PSI 引用提供器](src/main/resources/META-INF/plugin.xml#L9-L12)
- [Goto Declaration 处理器](src/main/resources/META-INF/plugin.xml#L13-L14)
- [Markdown 预览链接打开服务覆盖](src/main/resources/META-INF/plugin.xml#L15-L18)

这三条入口分别覆盖编辑器引用导航、编辑器声明跳转和预览面板点击跳转。

### 2. 编辑器里的 Ctrl+Click 是怎么工作的

[MarkdownCodeLinkReferenceContributor.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt#L11-L56) 会对 Markdown PSI 文本节点做过滤，只在：

- 文件名是 `.md` 或 `.markdown`
- 当前元素是叶子节点
- 文本不是空文本且长度不过大

这些前提下尝试提取可跳转片段。

真正的范围提取在 [extractReferenceRanges](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L195-L214)，它会从：

- 整段裸文本
- Markdown 行内链接的 `(...)` 目标部分

中找出可以解析的目标，然后在 [resolve()](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt#L52-L55) 中生成可导航目标。

### 3. 编辑器里的 Ctrl+B 是怎么工作的

[MarkdownCodeLinkGotoDeclarationHandler.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt#L7-L35) 会根据当前光标位置：

- 算出所在行
- 取出整行文本
- 调用 [findTargetInLine](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L260-L293) 判断当前 offset 是否落在某个可识别链接范围内

找到目标后，再调用 [createNavigatableTarget](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L153-L159) 返回给 IDE。

### 4. 共享解析核心是怎么组织的

[MarkdownCodeLinkSupport.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L21-L293) 是整个插件的核心，主要分成四块：

- [MarkdownCodeLinkTarget](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L21-L26)
  - 表示解析后的路径、起始行、结束行、列号
- [MarkdownCodeLinkParser](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L28-L108)
  - 负责识别 `#L42`、`#L42-L60`、`:42:5`、`file://`
  - 会过滤 `http://`、`https://`、`mailto:` 这类非本地代码链接
- [MarkdownCodeLinkResolver](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L110-L145)
  - 负责把目标路径解析成 `VirtualFile`
- [navigateToRawTarget / navigateToResolvedTarget](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L220-L258)
  - 负责真正打开文件、跳行、跳列、选中范围

### 5. 预览模式为什么现在可以用了

最终稳定生效的实现不是前端脚本注入，而是覆盖了 JetBrains Markdown 预览使用的 [MarkdownLinkOpener](src/main/resources/META-INF/plugin.xml#L15-L18) 服务。

具体代码在 [MarkdownPreviewLinkOpener.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L9-L37)：

- [openLink(project, link, sourceFile)](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L15-L24)
  - 先尝试把链接当作本地代码链接处理
  - 成功时复用 [navigateToRawTarget](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L220-L229)
  - 失败时再按普通安全 URI 交给浏览器
- [isSafeLink](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L26-L36)
  - 限制只放行 `http`、`https`、`mailto`、`ftp`、`file`

这也是为什么现在编辑器和预览共享同一套跳转规则，而不是各自维护一套解析逻辑。

## 项目结构

- [build.gradle.kts](build.gradle.kts#L3-L45)
  - Gradle、Kotlin、IntelliJ Platform 配置
- [plugin.xml](src/main/resources/META-INF/plugin.xml#L1-L20)
  - 插件入口注册
- [MarkdownCodeLinkSupport.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt#L21-L293)
  - 共享核心
- [MarkdownCodeLinkReferenceContributor.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt#L11-L56)
  - 编辑器 `Ctrl+Click`
- [MarkdownCodeLinkGotoDeclarationHandler.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt#L7-L35)
  - 编辑器 `Ctrl+B`
- [MarkdownPreviewLinkOpener.kt](src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt#L9-L37)
  - 预览模式点击跳转

## 构建与调试

打开项目后等待 Gradle 同步完成，然后使用：

```powershell
.\gradlew.bat compileKotlin
.\gradlew.bat buildPlugin
.\gradlew.bat runIde
```

构建产物输出到：

```text
build/distributions/
```

## 目标 IDE 版本

当前项目依赖配置在 [build.gradle.kts](build.gradle.kts#L19-L23)：

```kotlin
intellijPlatform {
    androidStudio("2025.3.2.4")
    bundledPlugin("org.intellij.plugins.markdown")
}
```

如果你的本地 Android Studio 版本不同，可以修改 [build.gradle.kts](build.gradle.kts#L19-L23) 中的目标版本。
