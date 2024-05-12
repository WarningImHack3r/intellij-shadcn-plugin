package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElementType
import dev.blachut.svelte.lang.psi.SvelteHtmlAttribute
import dev.blachut.svelte.lang.psi.SvelteHtmlElementTypes

class SvelteClassReplacementVisitor(newClass: (String) -> String) : ClassReplacementVisitor(newClass) {
    override val attributePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(SvelteHtmlElementTypes.SVELTE_HTML_ATTRIBUTE)
    override val attributeValuePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE_VALUE)

    override fun classAttributeNameFromElement(element: PsiElement): String? {
        val attribute = element as? SvelteHtmlAttribute ?: return null
        return attribute.name
    }
}
