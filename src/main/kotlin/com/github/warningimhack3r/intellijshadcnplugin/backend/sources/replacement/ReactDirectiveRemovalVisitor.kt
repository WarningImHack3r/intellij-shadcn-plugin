package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.intellij.lang.javascript.JSElementTypes
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType

class ReactDirectiveRemovalVisitor(
    project: Project,
    val directiveValue: (String) -> Boolean
) : PsiRecursiveElementVisitor() {
    private val matchingElements = mutableListOf<SmartPsiElementPointer<PsiElement>>()
    private val smartPointerManager = SmartPointerManager.getInstance(project)

    private val elementsToRemove = PlatformPatterns.psiElement().andOr(
        PlatformPatterns.psiElement(JSTokenTypes.SEMICOLON),
        PlatformPatterns.psiElement(CustomHighlighterTokenType.WHITESPACE)
        // TODO: Find a way to remove newlines?
    )

    // TODO: ExpressionStatement.directive?
    private val directive = PlatformPatterns.psiElement(JSElementTypes.EXPRESSION_STATEMENT)
        .withChild(
            PlatformPatterns.psiElement(
                VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                    "LITERAL_EXPRESSION",
                    $$"com.intellij.lang.javascript.JSElementTypes$Companion", // 2025.2+
                    "com.intellij.lang.javascript.JSElementTypes"
                ) ?: throw IllegalArgumentException("Import failed for LITERAL_EXPRESSION")
            )
        )

    private var directiveFound = false
    private var done = false

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (!done) {
            val isDirectiveCandidate = directive.accepts(element)
            val isJunk = elementsToRemove.accepts(element)

            if (!directiveFound && isDirectiveCandidate && directiveValue(
                    element.text.replace(
                        Regex("['\";]"),
                        ""
                    )
                )
            ) {
                matchingElements.add(smartPointerManager.createSmartPsiElementPointer(element))
                directiveFound = true
            } else if (directiveFound) {
                if (isJunk) {
                    matchingElements.add(smartPointerManager.createSmartPsiElementPointer(element))
                } else {
                    done = true
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun removeMatchingElements() {
        matchingElements.forEach { element ->
            val psiElement = runReadAction { element.dereference() } ?: return
            PsiHelper.writeAction(runReadAction { psiElement.containingFile }) {
                psiElement.delete()
            }
        }
    }
}
