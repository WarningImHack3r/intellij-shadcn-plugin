package com.github.warningimhack3r.intellijshadcnplugin

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.SvelteClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.VueClassReplacementVisitor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class ClassReplacementTests : BasePlatformTestCase() {

    override fun getTestDataPath() = "src/test/testData/classReplacement"

    private fun createVisitor(
        visitorClass: KClass<out ClassReplacementVisitor>,
        project: Project,
        newClass: (String) -> String
    ): ClassReplacementVisitor {
        val constructor = visitorClass.primaryConstructor
        if (constructor != null && constructor.parameters.size == 2) {
            return constructor.call(project, newClass)
        } else {
            throw IllegalArgumentException("Invalid visitor class. It should have a primary constructor with two parameters.")
        }
    }

    private fun beforeAndAfterContents(fileName: String): Pair<String, String> {
        val file = myFixture.configureByFile(fileName)
        val visitorClass = when (val extension = file.name.substringAfterLast('.')) {
            "jsx", "tsx", "js", "ts" -> JSXClassReplacementVisitor::class
            "svelte" -> SvelteClassReplacementVisitor::class
            "vue" -> VueClassReplacementVisitor::class
            else -> throw IllegalArgumentException("Unsupported extension: $extension")
        }
        val visitor = createVisitor(visitorClass, project) { "a-$it" }
        file.accept(visitor)
        return Pair(myFixture.configureByFile(fileName.let {
            it.substringBeforeLast('.') + "_after." + it.substringAfterLast('.')
        }).text, file.text)
    }

    fun testBasicClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("basic.tsx")
        assertEquals(expected, actual)
    }

    fun testSingleQuotesClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("singleQuotes.tsx")
        assertEquals(expected, actual)
    }

    fun testSvelteClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("basic.svelte")
        assertEquals(expected, actual)
    }

    fun testVueClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("basic.vue")
        assertEquals(expected, actual)
    }

    fun testNestedAndOtherAttributesReplacement() {
        val (expected, actual) = beforeAndAfterContents("nestedAndOtherAttributes.jsx")
        assertEquals(expected, actual)
    }

    fun testOtherAttributeReplacement() {
        val (expected, actual) = beforeAndAfterContents("simpleOtherAttribute.jsx")
        assertEquals(expected, actual)
    }

    fun testFullSimpleSvelteComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullSimple.svelte")
        assertEquals(expected, actual)
    }

    fun testFullComplexSvelteComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullComplex.svelte")
        assertEquals(expected, actual)
    }

    fun testFullSvelteVueTSClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullSvelteVue.ts")
        assertEquals(expected, actual)
    }

    fun testFullSvelteVueJSClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullSvelteVue.js")
        assertEquals(expected, actual)
    }

    fun testFullSimpleVueComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullSimple.vue")
        assertEquals(expected, actual)
    }

    fun testFullComplexVueComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullComplex.vue")
        assertEquals(expected, actual)
    }

    fun testFullSimpleJSXComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullSimple.tsx")
        assertEquals(expected, actual)
    }

    fun testFullComplexJSXComponentClassReplacement() {
        val (expected, actual) = beforeAndAfterContents("fullComplex.tsx")
        assertEquals(expected, actual)
    }
}
