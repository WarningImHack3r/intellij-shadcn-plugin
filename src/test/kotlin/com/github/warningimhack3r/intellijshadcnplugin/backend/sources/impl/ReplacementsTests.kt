package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class ReplacementsTests : BasePlatformTestCase() {

    /**
     * Creates a function that compares two imports in a file.
     *
     * @param transformer A function that modifies the file before comparison.
     * @param extension The file extension.
     * @param fileShape The file shape with a placeholder for the import. The placeholder is `{{import}}`.
     *
     * @return A function that compares two imports in a file, given the expected then actual imports.
     */
    fun compareImportsBuilder(
        transformer: (PsiFile) -> Unit,
        extension: String,
        fileShape: String
    ): (String, String) -> Unit {
        return { expected, actual ->
            val psiFile = myFixture.configureByText(
                "App.$extension", fileShape.replace("{{import}}", actual)
            )
            transformer(psiFile)
            assertEquals(
                fileShape.replace("{{import}}", expected),
                psiFile.text
            )
        }
    }

    fun testDummy() {
        assertTrue(true)
    }
}
