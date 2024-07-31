package com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.replacement

import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.xml.XmlAttributeImpl
import com.intellij.psi.xml.XmlElementType

class VueClassReplacementVisitor(project: Project) : ClassReplacementVisitor(project) {
    override val attributePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE)
    override val attributeValuePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE_VALUE)

    override fun classAttributeNameFromElement(element: PsiElement): String? {
        val attribute = element as? XmlAttributeImpl ?: return null
        return attribute.name
    }
}
