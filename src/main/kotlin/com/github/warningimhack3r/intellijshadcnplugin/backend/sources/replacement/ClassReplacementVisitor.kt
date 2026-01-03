package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.tree.IElementType
import com.intellij.psi.xml.XmlTokenType

abstract class ClassReplacementVisitor(project: Project) : PsiRecursiveElementVisitor() {
    private val matchingElements = mutableListOf<SmartPsiElementPointer<PsiElement>>()
    private val smartPointerManager = SmartPointerManager.getInstance(project)

    abstract val attributePattern: PsiElementPattern.Capture<PsiElement>
    abstract val attributeValuePattern: PsiElementPattern.Capture<PsiElement>

    private val attributeValueTokenPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)
    private val jsLiteralExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(
            VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                "LITERAL_EXPRESSION",
                $$"com.intellij.lang.javascript.JSElementTypes$Companion", // 2025.2+
                "com.intellij.lang.javascript.JSElementTypes"
            ) ?: throw IllegalArgumentException("Import failed for LITERAL_EXPRESSION")
        )
    private val jsCallExpressionPattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement(
            VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                "CALL_EXPRESSION",
                $$"com.intellij.lang.javascript.JSElementTypes$Companion", // 2025.2+
                "com.intellij.lang.javascript.JSElementTypes"
            ) ?: throw IllegalArgumentException("Import failed for CALL_EXPRESSION")
        )
    private val jsOrTsVariablePattern: PsiElementPattern.Capture<PsiElement> =
        PlatformPatterns.psiElement().andOr(
            PlatformPatterns.psiElement(
                VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                    "TYPESCRIPT_VARIABLE",
                    $$"com.intellij.lang.typescript.TypeScriptElementTypes$Companion", // 2025.2+
                    "com.intellij.lang.typescript.TypeScriptElementTypes"
                ) ?: throw IllegalArgumentException("Import failed for TYPESCRIPT_VARIABLE")
            ),
            PlatformPatterns.psiElement(
                VisitorHelpers.loadStaticFieldWithFallbacks<IElementType>(
                    "VARIABLE",
                    $$"com.intellij.lang.javascript.JSElementTypes$Companion", // 2025.2+
                    "com.intellij.lang.javascript.JSElementTypes"
                ) ?: throw IllegalArgumentException("Import failed for VARIABLE")
            )
        )

    abstract fun classAttributeNameFromElement(element: PsiElement): String?

    private fun depthRecurseChildren(element: PsiElement, action: (PsiElement) -> Unit) {
        element.children.forEach { child ->
            action(child)
            depthRecurseChildren(child, action)
        }
    }

    private fun saveMatchingElement(psiElement: PsiElement) {
        matchingElements.add(
            smartPointerManager.createSmartPsiElementPointer(
                psiElement,
                psiElement.containingFile
            )
        )
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
                saveMatchingElement(element)
            }

            jsLiteralExpressionPattern
                .withAncestor(2, jsCallExpressionPattern)
                .withAncestor(5, attributeValuePattern)
                .withAncestor(6, attributePattern)
                .accepts(element) -> {
                // String literal directly inside a function call
                saveMatchingElement(element)
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
                            saveMatchingElement(child)
                        }
                    }
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    fun replaceClasses(newText: (String) -> String) {
        matchingElements.forEach { element ->
            val psiElement = runReadAction { element.dereference() } ?: return@forEach
            replaceClassName(psiElement, newText)
        }
    }

    private fun replaceClassName(element: PsiElement, newText: (String) -> String) {
        val text = runReadAction { element.text }
        val quote = when (text.first()) {
            '\'', '`', '"' -> text.first().toString()
            else -> ""
        }
        val classes = text
            .split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") {
                if (quote.isEmpty()) {
                    newText(it)
                } else newText(it.replace(Regex(quote), ""))
            }
        val newElement = runReadAction {
            JSPsiElementFactory.createJSExpression("$quote$classes$quote", element)
        }
        PsiHelper.writeAction(runReadAction { element.containingFile }) {
            element.replace(newElement)
        }
    }
}
