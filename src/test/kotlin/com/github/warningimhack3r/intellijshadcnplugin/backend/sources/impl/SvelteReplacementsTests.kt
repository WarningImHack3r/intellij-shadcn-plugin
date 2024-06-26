package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SvelteReplacementsTests : ReplacementsTests() {

    class SvelteSourceStub(project: Project) : SvelteSource(project) {
        override fun getLocalConfig() = SvelteConfig(
            style = "default",
            tailwind = SvelteConfig.Tailwind(
                config = "tailwind.config.ts",
                css = "src/app.pcss",
                baseColor = "slate"
            ),
            aliases = SvelteConfig.Aliases(
                components = "\$lib/components",
                utils = "\$lib/replacedUtils"
            )
        )

        public override fun adaptFileToConfig(file: PsiFile) {
            super.adaptFileToConfig(file)
        }
    }

    private val compareImports by lazy {
        compareImportsBuilder(
            SvelteSourceStub(project)::adaptFileToConfig,
            "svelte",
            """
            <script>
                import { foo } from "{{import}}";
            </script>
            """.trimIndent()
        )
    }

    fun testImportMatchingComponentsAlias() {
        compareImports("\$lib/components/foo", "\$lib/registry/default/foo")
    }

    fun testImportNotMatchingComponentAlias1() {
        compareImports("\$lib/components", "\$lib/registry/foo")
    }

    fun testImportNotMatchingComponentAlias2() {
        compareImports("\$lib/notregistry/something/foo", "\$lib/notregistry/something/foo")
    }

    fun testImportMatchingUtilsAlias() {
        compareImports("\$lib/replacedUtils", "\$lib/utils")
    }

    fun testImportMatchingUtilsAlias2() {
        compareImports("\$lib/replacedUtils.js", "\$lib/utils.js")
    }

    fun testImportNotMatchingUtilsAlias() {
        compareImports("\$lib/notUtils", "\$lib/notUtils")
    }
}
