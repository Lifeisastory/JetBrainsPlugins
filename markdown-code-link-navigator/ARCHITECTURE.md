# 项目架构索引

## 项目类型
- 推断: JetBrains / Android Studio 单模块插件项目，定位为 Markdown 本地代码链接导航增强插件。
- 证据: `build.gradle.kts` 使用 `org.jetbrains.intellij.platform`；`src/main/resources/META-INF/plugin.xml` 注册 IntelliJ 扩展；源码集中在 `src/main/kotlin/com/melot/markdownnavigator/`。
- 置信度: high

## 顶层架构树
- `build.gradle.kts`: Gradle Kotlin DSL 构建入口，定义 Kotlin/JVM、IntelliJ Platform、目标 IDE 与插件元数据。
- `settings.gradle.kts`: Gradle 根工程命名，确认当前仓库不是多模块结构。
- `gradle.properties`: 构建级共享属性，当前作用较轻。
- `gradle/`: Gradle Wrapper 支撑目录。
- `src/main/resources/META-INF/plugin.xml`: 插件注册中心，声明扩展点与运行期接线。
- `src/main/kotlin/com/melot/markdownnavigator/`: 插件全部业务代码，当前只有 4 个 Kotlin 文件，边界非常集中。
- `build/`: 构建输出目录，应视为派生产物而非源码模块。
- `.gradle/`、`.intellijPlatform/`、`.kotlin/`: 本地缓存与工具目录，分析时应降权。

## 核心入口
- `src/main/resources/META-INF/plugin.xml`: 真正的运行时入口，向 IDE 注册 `psi.referenceContributor`、`gotoDeclarationHandler` 和 `MarkdownLinkOpener` 覆盖实现。
- `build.gradle.kts`: 构建与打包入口，声明目标平台为 `androidStudio("2025.3.2.4")`，并依赖 `org.intellij.plugins.markdown`。

## 模块边界
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt`: 共享核心模块，负责链接解析、路径解析、导航对象创建、编辑器跳转与范围识别，是项目的高耦合中心。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt`: 编辑器引用贡献模块，为 Markdown PSI 叶子节点提供可点击引用，实现 `Ctrl+Click`。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt`: 声明跳转模块，根据当前光标所在行提取目标，实现 `Ctrl+B` / Goto Declaration。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt`: 预览面板链接打开模块，优先复用共享导航逻辑，失败时再回退到浏览器打开安全 URI。
- `src/main/resources/META-INF/plugin.xml`: 配置接线模块，负责把上述运行时能力暴露给 IDE。

## 关键配置
- `build.gradle.kts`: 配置 Kotlin 2.1.21、JVM 17、IntelliJ Platform 2.12.0、插件名称、描述和最小构建版本 `253`。
- `src/main/resources/META-INF/plugin.xml`: 配置插件 id、名称、依赖模块与扩展实现类。
- `settings.gradle.kts`: 配置根项目名 `markdown-code-link-navigator`。

## 高耦合枢纽文件
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt`: 项目所有主要流程都会显式依赖它，证据是其公开的解析、查找、导航函数被另外 3 个 Kotlin 文件直接调用；置信度 high。
- `src/main/resources/META-INF/plugin.xml`: 不参与业务逻辑，但控制所有运行时装配；若这里改错，插件功能会整体失效；置信度 high。

## 主要流程骨架
- 插件激活路径: `build.gradle.kts -> src/main/resources/META-INF/plugin.xml -> IDE 扩展点装配`。
- 编辑器引用导航路径: `plugin.xml -> MarkdownCodeLinkReferenceContributor.kt -> MarkdownCodeLinkSupport.kt.extractReferenceRanges/parse/resolve -> MarkdownCodeLinkNavigatableElement.navigate`。
- Goto Declaration 路径: `plugin.xml -> MarkdownCodeLinkGotoDeclarationHandler.kt -> MarkdownCodeLinkSupport.kt.findTargetInLine/createNavigatableTarget -> navigateToResolvedTarget`。
- Markdown 预览点击路径: `plugin.xml -> MarkdownPreviewLinkOpener.kt -> MarkdownCodeLinkSupport.kt.navigateToRawTarget`，失败时回退到 `BrowserUtil.browse`。
- 路径解析路径: `MarkdownCodeLinkParser.parse -> MarkdownCodeLinkResolver.resolve -> LocalFileSystem.refreshAndFindFileByIoFile`。

## 关系摘要
- `MarkdownCodeLinkReferenceContributor.kt` depends_on `MarkdownCodeLinkSupport.kt` | explicit | 直接调用 `isMarkdownFileName`、`extractReferenceRanges`、`MarkdownCodeLinkParser.parse`、`createNavigatableTarget` | high
- `MarkdownCodeLinkGotoDeclarationHandler.kt` depends_on `MarkdownCodeLinkSupport.kt` | explicit | 直接调用 `isMarkdownFileName`、`findTargetInLine`、`createNavigatableTarget` | high
- `MarkdownPreviewLinkOpener.kt` depends_on `MarkdownCodeLinkSupport.kt` | explicit | 直接调用 `findBestMarkdownContextFile`、`navigateToRawTarget` | high
- `MarkdownCodeLinkSupport.kt` influenced_by `src/main/resources/META-INF/plugin.xml` | inferred | 该文件中的扩展注册决定其哪些能力会在 IDE 生命周期中被触发 | medium
- `src/main/resources/META-INF/plugin.xml` impacts `MarkdownCodeLinkReferenceContributor.kt`、`MarkdownCodeLinkGotoDeclarationHandler.kt`、`MarkdownPreviewLinkOpener.kt` | explicit | manifest wiring | high

## 推荐阅读顺序
- `src/main/resources/META-INF/plugin.xml`: 先看运行时接线，最快理解插件暴露了哪些能力。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt`: 再看共享核心，可一次性建立解析、解析后目标结构、文件定位和导航模型。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt`: 了解编辑器内 `Ctrl+Click` 如何接入 PSI。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt`: 了解 `Ctrl+B` 的单行定位逻辑。
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt`: 最后看预览面板路径，确认它如何复用共享核心。
- `build.gradle.kts`: 若需要发版、升级 IDE 版本或增加依赖，再回头看构建配置。

## 风险与未知项
- `README.md`: 当前文件看起来存在编码异常，内容可读性受损；如需确认历史设计意图，建议以编辑器 UTF-8/GBK 编码切换复核。
- `src/test/`: 当前仓库未看到测试目录，导航解析与 IDE 行为兼容性暂时缺少自动化保障。
- 运行期行为: 目前仅从源码与注册关系判断流程，尚未通过 `runIde` 实测预览面板和编辑器行为。
- 目标平台兼容性: `build.gradle.kts` 将目标锁定到 Android Studio `2025.3.2.4` 和 since-build `253`，更早 IDE 是否兼容尚未验证。

## 下一步最值得展开的点
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt`: 如果你接下来要改功能，这个文件最值得做二层细化。
- “链接解析规则”: 如果你关心支持哪些 Markdown 链接格式，优先细化 `MarkdownCodeLinkParser` 与 `findTargetInLine`。
- “预览面板行为”: 如果你关心点击预览为何成功或失败，优先细化 `MarkdownPreviewLinkOpener.kt` 与 `plugin.xml` 的服务覆盖关系。
