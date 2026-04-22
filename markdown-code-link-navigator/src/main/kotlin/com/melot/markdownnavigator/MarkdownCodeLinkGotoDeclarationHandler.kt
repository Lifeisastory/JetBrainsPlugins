package com.melot.markdownnavigator

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

class MarkdownCodeLinkGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor,
    ): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val file = element.containingFile ?: return null
        if (!isMarkdownFileName(file.name)) {
            return null
        }

        val document = editor.document
        if (offset < 0 || offset > document.textLength) {
            return null
        }

        val lineNumber = document.getLineNumber(offset.coerceAtMost(document.textLength))
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.charsSequence.subSequence(lineStart, lineEnd).toString()
        val lineOffset = offset - lineStart

        val target = findTargetInLine(lineText, lineOffset) ?: return null
        val navigatable = createNavigatableTarget(file, target) ?: return null
        return arrayOf(navigatable)
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String? = null
}
