package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.intellij.lang.ecmascript6.ES6StubElementTypes
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer

class ImportsPackagesReplacementVisitor(project: Project) : PsiRecursiveElementVisitor() {
    private val matchingElements = mutableListOf<SmartPsiElementPointer<PsiElement>>()
    private val smartPointerManager = SmartPointerManager.getInstance(project)

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (PlatformPatterns.psiElement(JSTokenTypes.STRING_LITERAL)
                .withParent(PlatformPatterns.psiElement(ES6StubElementTypes.FROM_CLAUSE))
                .accepts(element)
        ) {
            matchingElements.add(smartPointerManager.createSmartPsiElementPointer(element, element.containingFile))
        }
    }

    @Suppress("UnstableApiUsage")
    fun replaceImports(newText: (String) -> String) {
        runWriteAction {
            matchingElements.forEach { element ->
                val psiElement = element.dereference() ?: return@forEach
                replaceImport(psiElement, newText)
            }
        }
    }

    private fun replaceImport(element: PsiElement, newText: (String) -> String) {
        val quote = when (element.text.first()) {
            '\'', '`', '"' -> element.text.first().toString()
            else -> ""
        }
        val newImport = element.text.let {
            if (quote.isEmpty()) {
                // Cannot happen, but just in case
                newText(it)
            } else newText(it.replace(Regex(quote), ""))
        }
        val newElement = JSPsiElementFactory.createJSExpression("$quote$newImport$quote", element)
        PsiHelper.writeAction(element.containingFile) {
            element.replace(newElement)
        }
    }
}
