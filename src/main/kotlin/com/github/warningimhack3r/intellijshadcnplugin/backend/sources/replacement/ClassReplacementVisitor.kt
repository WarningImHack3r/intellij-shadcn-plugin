package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.lang.javascript.JSStubElementTypes
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.xml.XmlTokenType

abstract class ClassReplacementVisitor(
    private val project: Project,
    private val newClass: (String) -> String
) : PsiRecursiveElementVisitor() {
    abstract val attributePattern: PsiElementPattern.Capture<PsiElement>
    abstract val attributeValuePattern: PsiElementPattern.Capture<PsiElement>

    private val attributeValueTokenPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
    private val jsLiteralExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(JSStubElementTypes.LITERAL_EXPRESSION)
    private val jsCallExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(JSStubElementTypes.CALL_EXPRESSION)

    abstract fun classAttributeNameFromElement(element: PsiElement): String?

    private fun printChildren(element: PsiElement, depth: Int = 0) {
        element.children.forEach { child ->
            println("${">".repeat(depth)}Child: ${child.text} ($child/${child.javaClass})")
            printChildren(child, depth + 1)
        }
    }

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        println("Visit: ${element.text} ($element/${element.javaClass})\n\t\tParent: ${element.parent?.text} (${element.parent}/${element.parent?.javaClass})")

        if (jsLiteralExpressionPattern
                .withAncestor(2, jsCallExpressionPattern)
                .withAncestor(5, attributeValuePattern)
                .withAncestor(6, attributePattern)
                .accepts(element)
        ) {
            // String literal inside a function call
            println("Found valid expression call: '${element.text}' ($element/${element.javaClass})")
            printChildren(element)
            replaceClassName(element, newClass)
        } else if (attributeValueTokenPattern
                .withParent(attributeValuePattern)
                .withAncestor(2, attributePattern)
                .accepts(element)
        ) {
            // Regular string attribute value
            val attributeName = classAttributeNameFromElement(element.parent.parent) ?: return
            if (attributeName != "className" && attributeName != "class") return
            println("Found valid value: '${element.text}' ($element/${element.javaClass})")
            printChildren(element)
            replaceClassName(element.parent, newClass)
        }
    }

    private fun replaceClassName(element: PsiElement, newText: (String) -> String) {
        WriteCommandAction.runWriteCommandAction(project) {
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
            element.replace(newElement)
        }
    }
}
