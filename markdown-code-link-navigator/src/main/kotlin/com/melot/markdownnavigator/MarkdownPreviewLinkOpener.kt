package com.melot.markdownnavigator

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.plugins.markdown.ui.preview.accessor.MarkdownLinkOpener
import java.net.URI

@Suppress("OVERRIDE_DEPRECATION")
class MarkdownPreviewLinkOpener : MarkdownLinkOpener {
    private val logger = Logger.getInstance(MarkdownPreviewLinkOpener::class.java)

    override fun openLink(project: Project?, link: String) {
        openLink(project, link, null)
    }

    override fun openLink(project: Project?, link: String, sourceFile: VirtualFile?) {
        val contextFiles = project?.let { findMarkdownContextFiles(it, sourceFile) }.orEmpty()
        val normalizedLink = project?.let { toContextRelativeRawTarget(it, contextFiles, link) } ?: link

        logPreviewAttempt(
            project = project,
            link = link,
            sourceFile = sourceFile,
            contextFiles = contextFiles,
            normalizedLink = normalizedLink,
        )

        if (project != null && sourceFile != null && navigateToRawTarget(project, sourceFile, link)) {
            logPreviewStep("source-raw-hit", project, link, normalizedLink, sourceFile, contextFiles)
            return
        }

        if (project != null && navigateToRawTarget(project, contextFiles, link)) {
            logPreviewStep("context-raw-hit", project, link, normalizedLink, sourceFile, contextFiles)
            return
        }

        if (project != null && normalizedLink != link && sourceFile != null &&
            navigateToRawTarget(project, sourceFile, normalizedLink)
        ) {
            logPreviewStep("source-normalized-hit", project, link, normalizedLink, sourceFile, contextFiles)
            return
        }

        if (project != null && normalizedLink != link && navigateToRawTarget(project, contextFiles, normalizedLink)) {
            logPreviewStep("context-normalized-hit", project, link, normalizedLink, sourceFile, contextFiles)
            return
        }

        if (project != null && navigateToAbsoluteRawTarget(project, link)) {
            logPreviewStep("absolute-hit", project, link, normalizedLink, sourceFile, contextFiles)
            return
        }

        val uri = runCatching { URI(link) }.getOrNull()
        if (uri != null && isSafeLink(project, link)) {
            logPreviewStep("browser-fallback", project, link, normalizedLink, sourceFile, contextFiles)
            BrowserUtil.browse(uri)
            return
        }

        logPreviewStep("unhandled", project, link, normalizedLink, sourceFile, contextFiles)
    }

    override fun isSafeLink(project: Project?, link: String): Boolean {
        val uri = runCatching { URI(link) }.getOrNull() ?: return false
        if (!uri.isAbsolute) {
            return false
        }

        return when (uri.scheme?.lowercase()) {
            "http", "https", "mailto", "ftp", "file" -> true
            else -> false
        }
    }

    private fun logPreviewAttempt(
        project: Project?,
        link: String,
        sourceFile: VirtualFile?,
        contextFiles: List<VirtualFile>,
        normalizedLink: String,
    ) {
        logger.warn(
            buildString {
                append("preview-link attempt")
                append(" | project=")
                append(project?.name ?: "<null>")
                append(" | link=")
                append(link)
                append(" | normalizedLink=")
                append(normalizedLink)
                append(" | sourceFile=")
                append(sourceFile?.path ?: "<null>")
                append(" | contextFiles=")
                append(contextFiles.joinToString(prefix = "[", postfix = "]") { it.path })
            }
        )
    }

    private fun logPreviewStep(
        step: String,
        project: Project?,
        link: String,
        normalizedLink: String,
        sourceFile: VirtualFile?,
        contextFiles: List<VirtualFile>,
    ) {
        logger.warn(
            buildString {
                append("preview-link result")
                append(" | step=")
                append(step)
                append(" | project=")
                append(project?.name ?: "<null>")
                append(" | link=")
                append(link)
                append(" | normalizedLink=")
                append(normalizedLink)
                append(" | sourceFile=")
                append(sourceFile?.path ?: "<null>")
                append(" | contextCount=")
                append(contextFiles.size)
            }
        )
    }
}
