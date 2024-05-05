package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor

abstract class ClassReplacementVisitor(
    private val project: Project,
    private val newClass: (String) -> String
) : PsiRecursiveElementVisitor() {
    abstract val attributePattern: PsiElementPattern.Capture<PsiElement>
    abstract val attributeValuePattern: PsiElementPattern.Capture<PsiElement>

    abstract fun classAttributeNameFromElement(element: PsiElement): String?

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        println("Visit: ${element.text} ($element/${element.javaClass})\n\t\tParent: ${element.parent} (${element.parent?.javaClass})")

        if (attributeValuePattern.withParent(attributePattern).accepts(element)) {
            println("Found valid value: ${element.text} ($element/${element.javaClass})")
            val attributeName = classAttributeNameFromElement(element.parent) ?: return
            if (attributeName != "className" && attributeName != "class") return
            replaceClassName(element, newClass)
        }
    }

    private fun replaceClassName(element: PsiElement, newText: (String) -> String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val quote = when (element.text.first()) {
                '\'' -> "'"
                '`' -> "`"
                else -> "\""
            }
            val classes = element.text.split(" ").filter(CharSequence::isNotEmpty).joinToString(" ") {
                newText(it.replace(Regex(quote), ""))
            }
            val newElement = JSPsiElementFactory.createJSExpression("$quote$classes$quote", element)
            element.replace(newElement)
        }
    }
}
