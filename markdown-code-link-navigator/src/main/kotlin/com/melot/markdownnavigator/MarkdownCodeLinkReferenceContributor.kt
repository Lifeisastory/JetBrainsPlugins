package com.melot.markdownnavigator

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class MarkdownCodeLinkReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: com.intellij.psi.PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiElement::class.java),
            MarkdownCodeLinkReferenceProvider(),
        )
    }
}

private class MarkdownCodeLinkReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext,
    ): Array<PsiReference> {
        val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
        if (!isMarkdownFileName(file.name) || element.firstChild != null) {
            return PsiReference.EMPTY_ARRAY
        }

        val text = element.text ?: return PsiReference.EMPTY_ARRAY
        if (text.isBlank() || text.length > 2048) {
            return PsiReference.EMPTY_ARRAY
        }

        val references = extractReferenceRanges(text)
            .mapNotNull { range ->
                val rawTarget = range.substring(text)
                val target = MarkdownCodeLinkParser.parse(rawTarget) ?: return@mapNotNull null
                MarkdownCodeFileReference(element, range, target)
            }

        return references.toTypedArray()
    }
}

private class MarkdownCodeFileReference(
    element: PsiElement,
    rangeInElement: com.intellij.openapi.util.TextRange,
    private val target: MarkdownCodeLinkTarget,
) : PsiReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun resolve(): PsiElement? {
        val markdownFile = element.containingFile ?: return null
        return createNavigatableTarget(markdownFile, target)
    }
}
