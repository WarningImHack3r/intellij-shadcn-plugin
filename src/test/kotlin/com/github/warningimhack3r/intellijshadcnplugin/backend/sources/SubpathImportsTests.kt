package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language

class SubpathImportsTests : BasePlatformTestCase() {
    @Language("JSON")
    private val kit3 = """
        { "name": "app", "imports": { "#lib": "./src/lib/index.js", "#lib/*": "./src/lib/*" } }
    """.trimIndent()

    private fun resolve(@Language("JSON") packageJson: String, alias: String) =
        Source.resolveSubpathImport(packageJson, alias)

    fun testKit3Pattern() {
        assertEquals("src/lib/components", resolve(kit3, "#lib/components"))
    }

    fun testExactOverPattern() {
        assertEquals("src/lib/index.js", resolve(kit3, "#lib"))
    }

    fun testLongestPatternWins() {
        val json = """{ "imports": { "#lib/*": "./src/lib/*", "#lib/components/*": "./src/ui/*" } }"""
        assertEquals("src/ui/x", resolve(json, "#lib/components/x"))
        assertEquals("src/lib/utils", resolve(json, "#lib/utils"))
    }

    fun testSuffixPattern() {
        val json = """{ "imports": { "#lib/*.js": "./src/lib/*.ts" } }"""
        assertEquals("src/lib/utils.ts", resolve(json, "#lib/utils.js"))
        assertNull(resolve(json, "#lib/utils"))
    }

    fun testConditionalObject() {
        val json = """{ "imports": { "#lib/*": { "types": "./src/types/*", "default": "./src/lib/*" } } }"""
        assertEquals("src/lib/components", resolve(json, "#lib/components"))
        val importOnly = """{ "imports": { "#lib/*": { "types": "./src/types/*", "import": "./src/lib/*" } } }"""
        assertEquals("src/lib/components", resolve(importOnly, "#lib/components"))
        val firstString = """{ "imports": { "#lib/*": { "svelte": "./src/lib/*" } } }"""
        assertEquals("src/lib/components", resolve(firstString, "#lib/components"))
    }

    fun testConditionsMatchedInObjectOrder() {
        val importFirst = """{ "imports": { "#lib/*": { "import": "./src/esm/*", "default": "./src/lib/*" } } }"""
        assertEquals("src/esm/components", resolve(importFirst, "#lib/components"))
        val defaultFirst = """{ "imports": { "#lib/*": { "default": "./src/lib/*", "import": "./src/esm/*" } } }"""
        assertEquals("src/lib/components", resolve(defaultFirst, "#lib/components"))
        val inapplicableSkipped = """{ "imports": { "#lib/*": { "require": "./src/cjs/*", "types": "./src/types/*", "import": "./src/esm/*" } } }"""
        assertEquals("src/esm/components", resolve(inapplicableSkipped, "#lib/components"))
        val nested = """{ "imports": { "#lib/*": { "import": { "types": "./src/types/*", "default": "./src/esm/*" }, "default": "./src/lib/*" } } }"""
        assertEquals("src/esm/components", resolve(nested, "#lib/components"))
        assertNull(resolve("""{ "imports": { "#lib/*": { "require": "./src/cjs/*" } } }""", "#lib/components"))
        val moduleFirst = """{ "imports": { "#lib/*": { "module": "./src/esm/*", "default": "./src/lib/*" } } }"""
        assertEquals("src/esm/components", resolve(moduleFirst, "#lib/components"))
    }

    fun testNestedConditionWithoutMatchFallsThrough() {
        val noNestedMatch = """{ "imports": { "#lib/*": { "import": { "types": "./src/types/*" }, "default": "./src/lib/*" } } }"""
        assertEquals("src/lib/components", resolve(noNestedMatch, "#lib/components"))
        val nestedBlocked = """{ "imports": { "#lib/*": { "import": { "default": null }, "default": "./src/lib/*" } } }"""
        assertNull(resolve(nestedBlocked, "#lib/components"))
    }

    fun testNoDotSlashPrefix() {
        assertEquals("src/lib/components", resolve("""{ "imports": { "#lib/*": "src/lib/*" } }""", "#lib/components"))
    }

    fun testUnresolvable() {
        assertNull(resolve("""{ "name": "app" }""", "#lib/components"))
        assertNull(resolve(kit3, "#app/components"))
        assertNull(resolve("""{ "imports": { "#lib/*": ["./src/lib/*"] } }""", "#lib/components"))
        assertNull(resolve("""{ "imports": { "#lib/*": null } }""", "#lib/components"))
    }

    fun testBlockedKeyDoesNotFallBack() {
        assertNull(resolve("""{ "imports": { "#lib/x": null, "#lib/*": "./src/lib/*" } }""", "#lib/x"))
        assertNull(resolve("""{ "imports": { "#lib/ui/*": null, "#lib/*": "./src/lib/*" } }""", "#lib/ui/button"))
        assertEquals("src/lib/utils", resolve("""{ "imports": { "#lib/ui/*": null, "#lib/*": "./src/lib/*" } }""", "#lib/utils"))
    }
}
