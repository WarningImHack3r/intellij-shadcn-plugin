package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

class ComponentTests : BasePlatformTestCase() {
    private val testJson = Json

    fun testPolymorphicNoFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui" }]
        """.trimIndent()
        val result = testJson.decodeFromString(ListSerializer(ComponentDeserializer), input)
        assertSize(1, result)
        assertInstanceOf(result.first(), StringFileComponent::class.java)
    }

    fun testPolymorphicStringFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui", "files": ["index.js"] }]
        """.trimIndent()
        val result = testJson.decodeFromString(ListSerializer(ComponentDeserializer), input)
        assertSize(1, result)
        assertInstanceOf(result.first(), StringFileComponent::class.java)
    }

    fun testPolymorphicStructFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui", "files": [{ "path": "index.js", "type": "registry:component" }] }]
        """.trimIndent()
        val result = testJson.decodeFromString(ListSerializer(ComponentDeserializer), input)
        assertSize(1, result)
        assertInstanceOf(result.first(), StructFileComponent::class.java)
    }
}
