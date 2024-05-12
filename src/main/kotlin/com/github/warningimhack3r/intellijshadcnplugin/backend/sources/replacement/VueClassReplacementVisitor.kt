package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.xml.XmlAttributeImpl
import com.intellij.psi.xml.XmlElementType

class VueClassReplacementVisitor(newClass: (String) -> String) : ClassReplacementVisitor(newClass) {
    override val attributePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE)
    override val attributeValuePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE_VALUE)

    override fun classAttributeNameFromElement(element: PsiElement): String? {
        val attribute = element as? XmlAttributeImpl ?: return null
        return attribute.name
    }
}
