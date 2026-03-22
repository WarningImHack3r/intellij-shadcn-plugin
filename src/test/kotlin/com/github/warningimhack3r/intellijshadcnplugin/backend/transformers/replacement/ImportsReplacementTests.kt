package com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.replacement

import com.intellij.psi.PsiPlainTextFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath($$"$CONTENT_ROOT/src/test/testData")
class ImportsReplacementTests : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData/importsReplacement"

    private fun beforeAndAfterContents(fileName: String): Pair<String, String> {
        val file = myFixture.configureByFile(fileName)
        if (file is PsiPlainTextFile) {
            throw IllegalArgumentException("\"${file.name}\" is a plain text file; is the language supported?\nIf the language support comes from an external plugin, ensure it's properly installed and its mandatory dependencies are listed in your Bundled Plugins.")
        }

        val visitor = ImportsPackagesReplacementVisitor(project)
        file.accept(visitor)
        visitor.replaceImports { "a-$it" }
        return Pair(myFixture.configureByFile(fileName.let {
            it.substringBeforeLast('.') + "_after." + it.substringAfterLast('.')
        }).text, file.text)
    }

    fun testJSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("imports.js")
        assertEquals(expected, actual)
    }

    fun testTSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("imports.ts")
        assertEquals(expected, actual)
    }

    fun testJSXImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("imports.jsx")
        assertEquals(expected, actual)
    }

    fun testTSXImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("imports.tsx")
        assertEquals(expected, actual)
    }

    fun testSvelteTSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("importsTS.svelte")
        assertEquals(expected, actual)
    }

    fun testSvelteJSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("importsJS.svelte")
        assertEquals(expected, actual)
    }

    fun testVueTSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("importsTS.vue")
        assertEquals(expected, actual)
    }

    fun testVueJSImportsReplacement() {
        val (expected, actual) = beforeAndAfterContents("importsJS.vue")
        assertEquals(expected, actual)
    }
}
