package com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.replacement

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.tree.IElementType

class ImportsPackagesReplacementVisitor(project: Project) : PsiRecursiveElementVisitor() {
    private val matchingElements = mutableListOf<SmartPsiElementPointer<PsiElement>>()
    private val smartPointerManager = SmartPointerManager.getInstance(project)
    private val fromStringPattern = PlatformPatterns.psiElement(JSTokenTypes.STRING_LITERAL)
        .withParent(
            PlatformPatterns.psiElement(
                VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                    "FROM_CLAUSE",
                    "com.intellij.lang.javascript.JSElementTypes", // 2025.2+
                    "com.intellij.lang.ecmascript6.ES6StubElementTypes"
                ) ?: throw IllegalArgumentException("Import failed for FROM_CLAUSE")
            )
        )

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (fromStringPattern.accepts(element)) {
            matchingElements.add(
                smartPointerManager.createSmartPsiElementPointer(
                    element,
                    element.containingFile
                )
            )
        }
    }

    @Suppress("UnstableApiUsage")
    fun replaceImports(newText: (String) -> String) {
        matchingElements.forEach { element ->
            val psiElement = runReadAction { element.dereference() } ?: return@forEach
            replaceImport(psiElement, newText)
        }
    }

    private fun replaceImport(element: PsiElement, newText: (String) -> String) {
        val text = runReadAction { element.text }
        val quote = when (text.first()) {
            '\'', '`', '"' -> text.first().toString()
            else -> ""
        }
        val newImport = text.let {
            if (quote.isEmpty()) {
                // Cannot happen, but just in case
                newText(it)
            } else newText(it.replace(Regex(quote), ""))
        }
        val newElement = runReadAction {
            JSPsiElementFactory.createJSExpression("$quote$newImport$quote", element)
        }
        PsiHelper.writeAction(runReadAction { element.containingFile }) {
            element.replace(newElement)
        }
    }
}
