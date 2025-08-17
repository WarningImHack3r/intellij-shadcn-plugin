package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

class SvelteConfigTests : BasePlatformTestCase() {
    private val testJson = Json { ignoreUnknownKeys = true }

    fun testPolymorphicNoTS() {
        @Language("JSON")
        val input = """
            {
              "tailwind": { "css": "", "baseColor": "" }, "aliases": { "components": "", "utils": "" }
            }
        """.trimIndent()
        val result = testJson.decodeFromString(SvelteConfigDeserializer, input)
        assertInstanceOf(result, SvelteConfigTsBoolean::class.java)
    }

    fun testPolymorphicBoolTS() {
        @Language("JSON")
        val input = """
            {
              "tailwind": { "css": "", "baseColor": "" }, "aliases": { "components": "", "utils": "" },
              "typescript": false
            }
        """.trimIndent()
        val result = testJson.decodeFromString(SvelteConfigDeserializer, input)
        assertInstanceOf(result, SvelteConfigTsBoolean::class.java)
        assertFalse((result as SvelteConfigTsBoolean).typescript)
    }

    fun testPolymorphicObjTS() {
        @Language("JSON")
        val input = """
            {
              "tailwind": { "css": "", "baseColor": "" }, "aliases": { "components": "", "utils": "" },
              "typescript": { "config": "path" }
            }
        """.trimIndent()
        val result = testJson.decodeFromString(SvelteConfigDeserializer, input)
        assertInstanceOf(result, SvelteConfigTsObject::class.java)
        assertEquals("path", (result as SvelteConfigTsObject).typescript.config)
    }
}
