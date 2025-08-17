package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

class ComponentTests : BasePlatformTestCase() {
    private val testJson = Json { ignoreUnknownKeys = true }

    fun testPolymorphicNoFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui" }]
        """.trimIndent()
        val result = testJson.decodeFromString(
            ListSerializer(ComponentWithContentsDeserializer),
            input
        )
        assertSize(1, result)
        assertInstanceOf(result.first(), ComponentWithContentsLegacyFiles::class.java)
    }

    fun testPolymorphicLegacyFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui", "files": [{ "name": "index.js", "content": "..." }] }]
        """.trimIndent()
        val result = testJson.decodeFromString(
            ListSerializer(ComponentWithContentsDeserializer),
            input
        )
        assertSize(1, result)
        assertInstanceOf(result.first(), ComponentWithContentsLegacyFiles::class.java)
    }

    fun testPolymorphicNewFiles() {
        @Language("JSON")
        val input = """
            [{ "name": "accordion", "type": "registry:ui", "files": [{ "path": "index.js", "type": "registry:component" }] }]
        """.trimIndent()
        val result = testJson.decodeFromString(ListSerializer(ComponentWithContentsDeserializer), input)
        assertSize(1, result)
        assertInstanceOf(result.first(), ComponentWithContentsNewFiles::class.java)
    }
}
