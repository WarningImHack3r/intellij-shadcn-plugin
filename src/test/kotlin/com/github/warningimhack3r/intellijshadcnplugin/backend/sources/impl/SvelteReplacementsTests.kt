package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfigTsBoolean
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SvelteReplacementsTests : ReplacementsTests() {

    class SvelteSourceStub(
        project: Project,
        private val aliases: SvelteConfig.Aliases = SvelteConfig.Aliases(
            components = $$"$lib/replacedComponents",
            utils = $$"$lib/replacedUtils",
            ui = $$"$lib/components/replacedUi",
            hooks = $$"$lib/replacedHooks",
            lib = $$"$replacedLib",
        )
    ) : SvelteSource(project) {
        override fun getLocalConfig() = SvelteConfigTsBoolean(
            tailwind = SvelteConfig.Tailwind(
                css = "src/app.css",
                baseColor = "slate"
            ),
            aliases = aliases
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

    fun testImportMatchingAlias() {
        compareImports($$"$lib/replacedComponents", $$"$COMPONENTS$")
    }

    fun testImportContainingAlias() {
        compareImports($$"$lib/replacedHooks/myHook", $$"$HOOKS$/myHook")
    }

    fun testImportMatchingAnywhere() {
        compareImports($$"something/$replacedLib", $$"something/$LIB$")
    }

    fun testImportNotMatchingWrongCase() {
        compareImports($$"$ui$", $$"$ui$")
    }

    fun testImportNotMatchingUtilsAlias() {
        compareImports($$"$lib/notUtils", $$"$lib/notUtils")
    }

    fun testSubpathImportAliasesKeptVerbatim() {
        val stub = SvelteSourceStub(
            project, SvelteConfig.Aliases(
                components = "#lib/components",
                utils = "#lib/utils",
                ui = "#lib/components/ui",
                hooks = "#lib/hooks",
                lib = "#lib",
            )
        )
        val compare = compareImportsBuilder(
            stub::adaptFileToConfig,
            "svelte",
            """
            <script>
                import { foo } from "{{import}}";
            </script>
            """.trimIndent()
        )
        compare("#lib/utils", $$"$UTILS$")
        compare("#lib/components/ui/button", $$"$UI$/button")
    }
}
