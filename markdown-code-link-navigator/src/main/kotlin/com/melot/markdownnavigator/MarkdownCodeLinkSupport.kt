package com.melot.markdownnavigator

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import javax.swing.Icon
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class MarkdownCodeLinkTarget(
    val path: String,
    val startLine: Int? = null,
    val endLine: Int? = null,
    val column: Int? = null,
)

internal object MarkdownCodeLinkParser {
    private val knownCodeExtensions = setOf(
        "aidl", "bat", "c", "cc", "cpp", "cs", "css", "dart", "go", "gradle", "groovy",
        "h", "hpp", "html", "java", "js", "json", "jsx", "kt", "kts", "md", "mjs",
        "properties", "ps1", "py", "rs", "scss", "sh", "sql", "swift", "toml", "ts",
        "tsx", "txt", "xml", "yaml", "yml"
    )

    private val ignoredPrefixes = listOf(
        "http://", "https://", "mailto:", "ftp://", "tel:", "javascript:"
    )

    private val hashLinePattern = Regex(
        pattern = """^(?<path>.+?)#L(?<start>\d+)(?:-L?(?<end>\d+))?$""",
        option = RegexOption.IGNORE_CASE,
    )

    private val colonPattern = Regex(
        pattern = """^(?<path>.+?)(?::(?<line>\d+)(?::(?<column>\d+))?)?$""",
    )

    fun parse(rawValue: String): MarkdownCodeLinkTarget? {
        val normalizedRaw = extractInlineTarget(rawValue.trim())
        val candidate = normalizedRaw.trim().trim('<', '>')
        if (candidate.isEmpty() || candidate.startsWith("#")) {
            return null
        }

        val decoded = URLDecoder.decode(candidate, StandardCharsets.UTF_8).trim()
        if (ignoredPrefixes.any { decoded.startsWith(it, ignoreCase = true) }) {
            return null
        }

        hashLinePattern.matchEntire(decoded)?.let { match ->
            val path = match.groups["path"]?.value.orEmpty()
            if (!looksLikeLocalCodePath(path)) {
                return null
            }

            val startLine = match.groups["start"]?.value?.toIntOrNull() ?: return null
            val endLine = match.groups["end"]?.value?.toIntOrNull()

            return MarkdownCodeLinkTarget(
                path = path,
                startLine = startLine,
                endLine = endLine,
            )
        }

        val colonMatch = colonPattern.matchEntire(decoded) ?: return null
        val path = colonMatch.groups["path"]?.value.orEmpty()
        if (!looksLikeLocalCodePath(path)) {
            return null
        }

        return MarkdownCodeLinkTarget(
            path = path,
            startLine = colonMatch.groups["line"]?.value?.toIntOrNull(),
            column = colonMatch.groups["column"]?.value?.toIntOrNull(),
        )
    }

    private fun extractInlineTarget(rawValue: String): String {
        val inlineMatch = Regex("""^\[[^\]]*]\((.+)\)$""").matchEntire(rawValue)
        return inlineMatch?.groupValues?.get(1) ?: rawValue
    }

    private fun looksLikeLocalCodePath(path: String): Boolean {
        val normalized = path.substringBefore('?').substringBefore('#').trim()
        if (normalized.isEmpty()) {
            return false
        }

        if (normalized.startsWith("file://", ignoreCase = true)) {
            return true
        }

        val extension = normalized.substringAfterLast('.', "").lowercase()
        return extension in knownCodeExtensions
    }
}

internal object MarkdownCodeLinkResolver {
    fun resolve(markdownFile: PsiFile, target: MarkdownCodeLinkTarget): VirtualFile? {
        val markdownVirtualFile = markdownFile.virtualFile ?: return null
        return resolve(markdownFile.project, markdownVirtualFile, target)
    }

    fun resolve(project: Project, markdownFile: VirtualFile, target: MarkdownCodeLinkTarget): VirtualFile? {
        val rawPath = target.path.trim()
        if (rawPath.isEmpty()) {
            return null
        }

        val ioFile = runCatching { File(rawPath) }.getOrNull()
        if (ioFile != null && ioFile.isAbsolute) {
            return resolveLocalPath(ioFile)
        }

        val normalizedRelativePath = rawPath.replace('\\', '/')

        markdownFile.parent?.path
            ?.let { parentPath -> resolveLocalPath(File(parentPath, normalizedRelativePath)) }
            ?.let { return it }

        project.basePath
            ?.let { basePath -> resolveLocalPath(File(basePath, normalizedRelativePath)) }
            ?.let { return it }

        return null
    }

    fun resolveAbsolute(project: Project, target: MarkdownCodeLinkTarget): VirtualFile? {
        val rawPath = target.path.trim()
        if (rawPath.isEmpty()) {
            return null
        }

        val ioFile = runCatching { File(rawPath) }.getOrNull()
        if (ioFile != null && ioFile.isAbsolute) {
            return resolveLocalPath(ioFile)
        }

        if (rawPath.startsWith("file://", ignoreCase = true)) {
            return runCatching {
                resolveLocalPath(File(URI(rawPath)))
            }.getOrNull()
        }

        return project.basePath
            ?.let { basePath -> resolveLocalPath(File(basePath, rawPath.replace('\\', '/'))) }
    }

    private fun resolveLocalPath(file: File): VirtualFile? {
        val normalizedFile = runCatching { file.canonicalFile }.getOrElse { file.absoluteFile }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(normalizedFile)
    }
}

internal fun isMarkdownFileName(fileName: String): Boolean {
    val normalized = fileName.lowercase()
    return normalized.endsWith(".md") || normalized.endsWith(".markdown")
}

internal fun createNavigatableTarget(
    sourceFile: PsiFile,
    target: MarkdownCodeLinkTarget,
): PsiElement? {
    val targetFile = MarkdownCodeLinkResolver.resolve(sourceFile, target) ?: return null
    return MarkdownCodeLinkNavigatableElement(sourceFile.project, targetFile, target)
}

internal class MarkdownCodeLinkNavigatableElement(
    private val project: Project,
    private val targetFile: VirtualFile,
    private val target: MarkdownCodeLinkTarget,
) : FakePsiElement() {

    override fun getParent(): PsiElement? = getContainingFile()

    override fun getContainingFile(): PsiFile? = PsiManager.getInstance(project).findFile(targetFile)

    override fun getName(): String = targetFile.name

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = targetFile.name

        override fun getLocationString(): String = target.path

        override fun getIcon(unused: Boolean): Icon? =
            FileTypeManager.getInstance().getFileTypeByFile(targetFile).icon
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true

    override fun navigate(requestFocus: Boolean) {
        navigateToResolvedTarget(project, targetFile, target, requestFocus)
    }

    override fun isValid(): Boolean = targetFile.isValid

    override fun toString(): String = "MarkdownCodeLinkNavigatableElement($target)"
}

internal fun extractReferenceRanges(text: String): List<TextRange> {
    val ranges = linkedSetOf<TextRange>()

    MarkdownCodeLinkParser.parse(text)?.let {
        ranges += TextRange(0, text.length)
    }

    Regex("""\(([^()\r\n]+)\)""")
        .findAll(text)
        .forEach { match ->
            val rawTarget = match.groupValues[1]
            if (MarkdownCodeLinkParser.parse(rawTarget) != null) {
                val start = match.range.first + 1
                val end = start + rawTarget.length
                ranges += TextRange(start, end)
            }
        }

    return ranges.toList()
}

internal fun isNavigableMarkdownCodeLink(rawValue: String): Boolean {
    return MarkdownCodeLinkParser.parse(rawValue) != null
}

internal fun navigateToRawTarget(
    project: Project,
    markdownFile: VirtualFile,
    rawValue: String,
    requestFocus: Boolean = true,
): Boolean {
    val target = MarkdownCodeLinkParser.parse(rawValue) ?: return false
    val targetFile = MarkdownCodeLinkResolver.resolve(project, markdownFile, target) ?: return false
    navigateToResolvedTarget(project, targetFile, target, requestFocus)
    return true
}

internal fun navigateToAbsoluteRawTarget(
    project: Project,
    rawValue: String,
    requestFocus: Boolean = true,
): Boolean {
    val target = MarkdownCodeLinkParser.parse(rawValue) ?: return false
    val targetFile = MarkdownCodeLinkResolver.resolveAbsolute(project, target) ?: return false
    navigateToResolvedTarget(project, targetFile, target, requestFocus)
    return true
}

internal fun toProjectRelativeRawTarget(project: Project, rawValue: String): String? {
    val target = MarkdownCodeLinkParser.parse(rawValue) ?: return null
    val rawPath = target.path.trim()
    if (!rawPath.startsWith("file://", ignoreCase = true)) {
        return null
    }

    val absoluteFile = runCatching { File(URI(rawPath)).canonicalFile }.getOrNull() ?: return null
    val projectBase = project.basePath?.let { File(it).canonicalFile } ?: return null

    val absolutePath = absoluteFile.invariantSeparatorsPath
    val projectBasePath = projectBase.invariantSeparatorsPath.trimEnd('/')
    if (!absolutePath.startsWith("$projectBasePath/")) {
        return null
    }

    val relativePath = absolutePath.removePrefix("$projectBasePath/")
    return buildString {
        append(relativePath)
        target.startLine?.let { startLine ->
            append("#L")
            append(startLine)
            target.endLine?.let { endLine ->
                append("-L")
                append(endLine)
            }
        } ?: target.column?.let { column ->
            append(':')
            append(target.startLine ?: 1)
            append(':')
            append(column)
        }
    }
}

internal fun navigateToRawTarget(
    project: Project,
    markdownFiles: Iterable<VirtualFile>,
    rawValue: String,
    requestFocus: Boolean = true,
): Boolean {
    val target = MarkdownCodeLinkParser.parse(rawValue) ?: return false

    for (markdownFile in markdownFiles) {
        val targetFile = MarkdownCodeLinkResolver.resolve(project, markdownFile, target) ?: continue
        navigateToResolvedTarget(project, targetFile, target, requestFocus)
        return true
    }

    return false
}

internal fun findMarkdownContextFiles(project: Project, sourceFile: VirtualFile?): List<VirtualFile> {
    val editorManager = FileEditorManager.getInstance(project)
    return buildList {
        if (sourceFile != null && isMarkdownFileName(sourceFile.name)) {
            add(sourceFile)
        }

        editorManager.selectedFiles
            .filterTo(this) { isMarkdownFileName(it.name) }

        editorManager.openFiles
            .filterTo(this) { isMarkdownFileName(it.name) }
    }.distinctBy { it.path }
}

internal fun navigateToResolvedTarget(
    project: Project,
    targetFile: VirtualFile,
    target: MarkdownCodeLinkTarget,
    requestFocus: Boolean,
) {
    val targetLine = (target.startLine ?: 1).coerceAtLeast(1) - 1
    val targetColumn = (target.column ?: 1).coerceAtLeast(1) - 1
    val descriptor = OpenFileDescriptor(project, targetFile, targetLine, targetColumn)
    val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, requestFocus)

    val endLine = target.endLine ?: return
    if (editor == null || endLine < (target.startLine ?: endLine)) {
        return
    }

    val document = editor.document
    if (document.lineCount == 0) {
        return
    }

    val safeStartLine = targetLine.coerceIn(0, document.lineCount - 1)
    val safeEndLine = (endLine - 1).coerceIn(safeStartLine, document.lineCount - 1)
    val startOffset = document.getLineStartOffset(safeStartLine)
    val endOffset = document.getLineEndOffset(safeEndLine)
    editor.selectionModel.setSelection(startOffset, endOffset)
}

internal fun findTargetInLine(lineText: String, lineOffset: Int): MarkdownCodeLinkTarget? {
    val ranges = linkedSetOf<TextRange>()

    Regex("""\[[^\]]*]\(([^)\r\n]+)\)""")
        .findAll(lineText)
        .forEach { match ->
            val rawTarget = match.groupValues[1]
            val targetStart = match.range.first + match.value.indexOf('(') + 1
            val targetEnd = targetStart + rawTarget.length
            ranges += TextRange(targetStart, targetEnd)
            ranges += TextRange(match.range.first, match.range.last + 1)
        }

    Regex("""<([^<>\r\n]+)>""")
        .findAll(lineText)
        .forEach { match ->
            val rawTarget = match.groupValues[1]
            if (MarkdownCodeLinkParser.parse(rawTarget) != null) {
                val start = match.range.first + 1
                val end = start + rawTarget.length
                ranges += TextRange(match.range.first, match.range.last + 1)
                ranges += TextRange(start, end)
            }
        }

    val targetRange = ranges.firstOrNull { lineOffset in it.startOffset until it.endOffset } ?: return null
    val rawText = lineText.substring(targetRange.startOffset, targetRange.endOffset)

    return MarkdownCodeLinkParser.parse(rawText)
        ?: MarkdownCodeLinkParser.parse(
            rawText.substringAfter('(', missingDelimiterValue = rawText)
                .substringBeforeLast(')', missingDelimiterValue = rawText),
        )
}
