package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.intellij.lang.javascript.JSStubElementTypes
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.lang.typescript.TypeScriptStubElementTypes
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.xml.XmlTokenType

abstract class ClassReplacementVisitor(private val newClass: (String) -> String) : PsiRecursiveElementVisitor() {
    abstract val attributePattern: PsiElementPattern.Capture<PsiElement>
    abstract val attributeValuePattern: PsiElementPattern.Capture<PsiElement>

    private val attributeValueTokenPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
    private val jsLiteralExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(JSStubElementTypes.LITERAL_EXPRESSION)
    private val jsCallExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(JSStubElementTypes.CALL_EXPRESSION)
    private val jsOrTsVariablePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().andOr(
            PlatformPatterns.psiElement(TypeScriptStubElementTypes.TYPESCRIPT_VARIABLE),
            PlatformPatterns.psiElement(JSStubElementTypes.VARIABLE)
        )

    abstract fun classAttributeNameFromElement(element: PsiElement): String?

    private fun depthRecurseChildren(element: PsiElement, action: (PsiElement) -> Unit) {
        element.children.forEach { child ->
            action(child)
            depthRecurseChildren(child, action)
        }
    }

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        when {
            attributeValuePattern
                .withChild(attributeValueTokenPattern)
                .withAncestor(2, attributePattern)
                .accepts(element) -> {
                // Regular string attribute value
                val attributeName = classAttributeNameFromElement(element.parent) ?: return
                if (attributeName != "className" && attributeName != "class") return
                replaceClassName(element, newClass)
            }

            jsLiteralExpressionPattern
                .withAncestor(2, jsCallExpressionPattern)
                .withAncestor(5, attributeValuePattern)
                .withAncestor(6, attributePattern)
                .accepts(element) -> {
                // String literal directly inside a function call
                replaceClassName(element, newClass)
            }

            jsCallExpressionPattern
                .withChild(
                    PlatformPatterns.psiElement().andOr(
                        PlatformPatterns.psiElement().withText("tv"),
                        PlatformPatterns.psiElement().withText("cva")
                    )
                )
                .withParent(jsOrTsVariablePattern)
                .accepts(element) -> {
                // tailwind-variants & class-variance-authority
                depthRecurseChildren(element) { child ->
                    if (jsLiteralExpressionPattern.accepts(child)) {
                        val greatGrandparent = child.parent?.parent?.parent ?: return@depthRecurseChildren
                        if ((greatGrandparent is JSProperty && greatGrandparent.name != "defaultVariants")
                            || greatGrandparent !is JSProperty
                        ) {
                            replaceClassName(child, newClass)
                        }
                    }
                }
            }
        }
    }

    private fun replaceClassName(element: PsiElement, newText: (String) -> String) {
        val quote = when (element.text.first()) {
            '\'', '`', '"' -> element.text.first().toString()
            else -> ""
        }
        val classes = element.text
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") {
                if (quote.isEmpty()) {
                    newText(it)
                } else newText(it.replace(Regex(quote), ""))
            }
        val newElement = JSPsiElementFactory.createJSExpression("$quote$classes$quote", element)
        PsiHelper.writeAction(element.containingFile) {
            element.replace(newElement)
        }
    }
}
