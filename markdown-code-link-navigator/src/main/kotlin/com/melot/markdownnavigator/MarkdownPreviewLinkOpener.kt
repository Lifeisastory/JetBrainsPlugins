package com.melot.markdownnavigator

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.plugins.markdown.ui.preview.accessor.MarkdownLinkOpener
import java.net.URI

@Suppress("OVERRIDE_DEPRECATION")
class MarkdownPreviewLinkOpener : MarkdownLinkOpener {
    override fun openLink(project: Project?, link: String) {
        openLink(project, link, null)
    }

    override fun openLink(project: Project?, link: String, sourceFile: VirtualFile?) {
        val normalizedLink = project?.let { toProjectRelativeRawTarget(it, link) } ?: link

        val contextFiles = project?.let { findMarkdownContextFiles(it, sourceFile) }.orEmpty()
        if (project != null && navigateToRawTarget(project, contextFiles, normalizedLink)) {
            return
        }

        if (project != null && normalizedLink !== link && navigateToAbsoluteRawTarget(project, link)) {
            return
        }

        val uri = runCatching { URI(link) }.getOrNull()
        if (uri != null && isSafeLink(project, link)) {
            BrowserUtil.browse(uri)
        }
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
}
