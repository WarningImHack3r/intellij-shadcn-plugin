package com.github.warningimhack3r.intellijshadcnplugin

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class ImportsReplacementTests : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData/importsReplacement"

    private fun beforeAndAfterContents(fileName: String): Pair<String, String> {
        val file = myFixture.configureByFile(fileName)
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
