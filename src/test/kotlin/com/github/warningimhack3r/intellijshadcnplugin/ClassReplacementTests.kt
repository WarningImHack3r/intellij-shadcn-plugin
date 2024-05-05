package com.github.warningimhack3r.intellijshadcnplugin

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.SvelteClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.VueClassReplacementVisitor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class ClassReplacementTests : BasePlatformTestCase() {

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

    private fun processedFile(contents: String, extension: String): String {
        val file = myFixture.configureByText("file.${extension.removePrefix(".")}", contents)
        val visitorClass = when (extension) {
            "jsx", "tsx" -> JSXClassReplacementVisitor::class
            "svelte" -> SvelteClassReplacementVisitor::class
            "vue" -> VueClassReplacementVisitor::class
            else -> throw IllegalArgumentException("Unsupported extension: $extension")
        }
        val visitor = createVisitor(visitorClass, project) { "a-$it" }
        file.accept(visitor)
        return file.text
    }

    fun testBasicClassReplacement() {
        val fileContent = """
            const tag = <div className="foo">Hello, world!</div>;
        """.trimIndent()

        val expected = """
            const tag = <div className="a-foo">Hello, world!</div>;
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "tsx"))
    }

    fun testSingleQuotesClassReplacement() {
        val fileContent = """
            const tag = <div className='foo'>Hello, world!</div>;
        """.trimIndent()

        val expected = """
            const tag = <div className='a-foo'>Hello, world!</div>;
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "tsx"))
    }

    fun testSvelteClassReplacement() {
        val fileContent = """
            <script>
                let name = "world";
            </script>
    
            <h1 class="hello">Hello {name}!</h1>
        """.trimIndent()

        val expected = """
            <script>
                let name = "world";
            </script>
    
            <h1 class="a-hello">Hello {name}!</h1>
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "svelte"))
    }

    fun testVueClassReplacement() {
        val fileContent = """
            <template>
                <div id="app" class="foo">
                    {{ message }}
                </div>
            </template>
    
            <script>
                export default {
                    data() {
                        return {
                            message: "Hello Vue!"
                        }
                    }
                }
            </script>
        """.trimIndent()

        val expected = """
            <template>
                <div id="app" class="a-foo">
                    {{ message }}
                </div>
            </template>
    
            <script>
                export default {
                    data() {
                        return {
                            message: "Hello Vue!"
                        }
                    }
                }
            </script>
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "vue"))
    }

    fun testComprehensiveFileClassReplacement() {
        val fileContent = """
            <div class="foo">
                <span class="bar">Hello, world!</span>
                <p id="baz" class="qux">Goodbye, world!</p>
            </div>
        """.trimIndent()

        val expected = """
            <div class="a-foo">
                <span class="a-bar">Hello, world!</span>
                <p id="baz" class="a-qux">Goodbye, world!</p>
            </div>
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "jsx"))
    }

    fun testOtherAttributeReplacement() {
        val fileContent = """
            <div id="foo">Hello, world!</div>
        """.trimIndent()

        val expected = """
            <div id="foo">Hello, world!</div>
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "jsx"))
    }

    fun testMixAndMatchReplacement() {
        val fileContent = """
            <div class="foo" id="bar">
                <span class="baz" id="qux">Hello, world!</span>
            </div>
        """.trimIndent()

        val expected = """
            <div class="a-foo" id="bar">
                <span class="a-baz" id="qux">Hello, world!</span>
            </div>
        """.trimIndent()

        assertEquals(expected, processedFile(fileContent, "tsx"))
    }
}
