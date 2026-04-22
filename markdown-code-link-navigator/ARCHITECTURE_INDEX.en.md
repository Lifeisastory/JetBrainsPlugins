# Architecture Index

## Artifact Metadata
- Artifact: architecture-index
- Language: English
- Source: generated-from-project-scan

## Project Type
- Inference: library
- Evidence: `build.gradle.kts` applies `org.jetbrains.intellij.platform`; `src/main/resources/META-INF/plugin.xml` registers IntelliJ extensions; source code is concentrated in one Kotlin package under `src/main/kotlin/com/melot/markdownnavigator/`.
- Confidence: high

## Entrypoints
- `build.gradle.kts` | build and packaging entrypoint for the plugin | IntelliJ Platform Gradle configuration plus plugin metadata | high
- `src/main/resources/META-INF/plugin.xml` | runtime registration entrypoint | manifest wiring for reference contributor, goto declaration handler, and Markdown link opener override | high

## Modules
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | shared parsing, resolution, and navigation hub | direct function usage from the other Kotlin files | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt` | editor reference integration for Markdown PSI | registered in `plugin.xml` as `psi.referenceContributor` | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt` | goto declaration integration for cursor-based navigation | registered in `plugin.xml` as `gotoDeclarationHandler` | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | preview-panel link opener override | registered in `plugin.xml` as `applicationService` for `MarkdownLinkOpener` | high
- `src/main/resources/META-INF/plugin.xml` | runtime wiring module | extension registration and dependency declaration | high

## Files
- `build.gradle.kts` | Gradle Kotlin DSL build definition | declares Kotlin/JVM, IntelliJ Platform plugin, Android Studio target, and plugin metadata | high
- `settings.gradle.kts` | root project naming | defines single-module root name `markdown-code-link-navigator` | high
- `src/main/resources/META-INF/plugin.xml` | plugin manifest | explicit dependency and extension registration | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | core domain logic | contains target model, parser, resolver, navigatable element, and navigation helpers | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt` | PSI reference provider | converts Markdown text ranges into navigable references | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt` | editor declaration jump handler | resolves target from the current line and caret offset | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | preview link dispatcher | routes local code links to IDE navigation and safe absolute URIs to browser fallback | high

## Relationships
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt` | depends_on | `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | explicit | direct symbol usage: `isMarkdownFileName`, `extractReferenceRanges`, `MarkdownCodeLinkParser.parse`, `createNavigatableTarget` | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt` | depends_on | `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | explicit | direct symbol usage: `isMarkdownFileName`, `findTargetInLine`, `createNavigatableTarget` | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | depends_on | `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | explicit | direct symbol usage: `findBestMarkdownContextFile`, `navigateToRawTarget` | high
- `src/main/resources/META-INF/plugin.xml` | depended_on_by | `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt` | explicit | manifest wiring registers the class for Markdown reference contribution | high
- `src/main/resources/META-INF/plugin.xml` | depended_on_by | `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt` | explicit | manifest wiring registers the handler | high
- `src/main/resources/META-INF/plugin.xml` | depended_on_by | `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | explicit | manifest wiring overrides `MarkdownLinkOpener` with this implementation | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | influenced_by | `src/main/resources/META-INF/plugin.xml` | inferred | the helper functions are activated only through plugin extension wiring, not through standalone bootstrap code | medium
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | impacts | `editor reference navigation flow` | explicit | all editor and preview navigation paths eventually parse or resolve through this file | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | impacts | `preview click flow` | explicit | `navigateToRawTarget` is called by the preview opener | high

## Flows
- `plugin activation path` | `build.gradle.kts -> src/main/resources/META-INF/plugin.xml -> IDE extension loading` | high
- `editor reference navigation` | `src/main/resources/META-INF/plugin.xml -> MarkdownCodeLinkReferenceContributor.kt -> MarkdownCodeLinkSupport.kt -> MarkdownCodeLinkNavigatableElement.navigate` | high
- `goto declaration navigation` | `src/main/resources/META-INF/plugin.xml -> MarkdownCodeLinkGotoDeclarationHandler.kt -> MarkdownCodeLinkSupport.kt.findTargetInLine -> navigateToResolvedTarget` | high
- `preview link navigation` | `src/main/resources/META-INF/plugin.xml -> MarkdownPreviewLinkOpener.kt -> MarkdownCodeLinkSupport.kt.navigateToRawTarget -> BrowserUtil.browse fallback` | high
- `path resolution flow` | `MarkdownCodeLinkParser.parse -> MarkdownCodeLinkResolver.resolve -> LocalFileSystem.refreshAndFindFileByIoFile` | high

## Recommended Next Reads
- `src/main/resources/META-INF/plugin.xml` | best first file for runtime wiring and capability inventory
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | explains the shared target model, parser, resolver, and navigation behavior
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkReferenceContributor.kt` | shows how clickable references are attached to Markdown PSI
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkGotoDeclarationHandler.kt` | shows caret-based jump logic and line-level extraction
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | shows preview-specific dispatch and safe URI fallback
- `build.gradle.kts` | useful when changing platform target, plugin metadata, or dependencies

## Risks And Unknowns
- `README.md` | appears to have an encoding readability issue in the current shell output | `README.md`
- `test coverage` | no `src/test/` directory was observed during this scan, so parser and navigation behavior lack visible automated coverage | `src/`
- `runtime verification` | flows were inferred from code and manifest wiring without a `runIde` execution check | `src/main/resources/META-INF/plugin.xml`
- `IDE compatibility range` | target Android Studio version and `sinceBuild=253` are explicit, but backward compatibility beyond that bound is unverified | `build.gradle.kts`

## Confirmed Updates
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | preview-link normalization now converts project-local absolute URIs, including preview-resolved `../` sibling-folder paths, back into project-relative code targets before navigation | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | preview navigation compares normalized links by value, ensuring the absolute-link fallback only runs when normalization actually changed the target | code-confirmed | high
- `build.gradle.kts` | version declaration was deduplicated and bumped to `0.1.2` | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownPreviewLinkOpener.kt` | preview navigation now tries the current Markdown source file first, then other Markdown contexts, and only after that falls back to normalized or absolute target handling | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | URI normalization now prefers resolving preview links relative to the current Markdown directory and falls back to the project root only when that lookup fails | code-confirmed | high
- `build.gradle.kts` | version bumped again to `0.1.3` after the preview-resolution follow-up fix | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | preview normalization now also accepts localhost/file absolute URLs emitted by the Markdown preview layer and reduces them to relative code paths before navigation | code-confirmed | high
- `build.gradle.kts` | version bumped to `0.1.4` after fixing preview absolute-URL handling | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | sibling-folder preview targets now compute a true relative path from the current Markdown directory instead of requiring the target to stay under the same directory tree | code-confirmed | high
- `build.gradle.kts` | version bumped to `0.1.5` after fixing `../` sibling-folder preview navigation | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | preview normalization now also treats decoded filesystem absolute paths as local files, covering localhost-preview URLs whose paths were already stripped from their scheme | code-confirmed | high
- `build.gradle.kts` | version bumped to `0.1.6` after broadening preview absolute-path handling | code-confirmed | high
- `src/main/kotlin/com/melot/markdownnavigator/MarkdownCodeLinkSupport.kt` | preview absolute-URL parsing now preserves line fragments encoded in the path, fragment, or query, and root-relative `/...` paths no longer overuse suffix fallback candidates | code-confirmed | high
- `build.gradle.kts` | version bumped to `0.1.8` after fixing root-relative preview target parsing | code-confirmed | high
