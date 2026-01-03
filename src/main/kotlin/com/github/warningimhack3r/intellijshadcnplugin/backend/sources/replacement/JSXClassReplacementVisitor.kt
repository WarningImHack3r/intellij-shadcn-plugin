package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.lang.javascript.psi.e4x.impl.JSXmlAttributeImpl
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.xml.XmlElementType

class JSXClassReplacementVisitor(project: Project) : ClassReplacementVisitor(project) {
    override val attributePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(
            VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                "XML_ATTRIBUTE",
                $$"com.intellij.lang.javascript.JSElementTypes$Companion", // 2025.2+
                "com.intellij.lang.javascript.JSElementTypes"
            ) ?: throw IllegalArgumentException("Import failed for XML_ATTRIBUTE")
        )
    override val attributeValuePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlElementType.XML_ATTRIBUTE_VALUE)

    @Suppress("UnstableApiUsage")
    override fun classAttributeNameFromElement(element: PsiElement): String? {
        val attribute = element as? JSXmlAttributeImpl ?: return null
        return attribute.name
    }
}
