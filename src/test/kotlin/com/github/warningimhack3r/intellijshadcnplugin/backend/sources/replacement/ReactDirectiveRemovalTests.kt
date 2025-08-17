package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath($$"$CONTENT_ROOT/src/test/testData")
class ReactDirectiveRemovalTests : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData/reactDirectiveRemoval"

    private fun beforeAndAfterContents(fileName: String): Pair<String, String> {
        val file = myFixture.configureByFile(fileName)
        val visitor = ReactDirectiveRemovalVisitor(project) { true }
        file.accept(visitor)
        visitor.removeMatchingElements()
        return Pair(myFixture.configureByFile(fileName.let {
            it.substringBeforeLast('.') + "_after." + it.substringAfterLast('.')
        }).text, file.text)
    }

    fun testJSDirectiveRemoval() {
        val (expected, actual) = beforeAndAfterContents("react.jsx")
        assertEquals(expected, actual)
    }

    fun testTSDirectiveRemoval() {
        val (expected, actual) = beforeAndAfterContents("react.tsx")
        assertEquals(expected, actual)
    }
}
