package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.VueConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class VueReplacementsTests : ReplacementsTests() {
    private var useNotNullUI = false

    inner class VueSourceStub(project: Project) : VueSource(project) {
        override fun getLocalConfig() = VueConfig(
            style = "default",
            typescript = true,
            tailwind = VueConfig.Tailwind(
                config = "tailwind.config.js",
                css = "src/index.css",
                baseColor = "slate",
                cssVariables = true
            ),
            framework = VueConfig.Framework.NUXT,
            aliases = VueConfig.Aliases(
                components = "@/components",
                utils = "@/lib/utilsReplaced",
                ui = if (useNotNullUI) "@/ui" else null
            )
        )

        public override fun adaptFileToConfig(file: PsiFile) {
            super.adaptFileToConfig(file)
        }
    }

    private val compareImports by lazy {
        compareImportsBuilder(
            VueSourceStub(project)::adaptFileToConfig,
            "vue",
            """
            <script>
                import { foo } from "{{import}}";
            </script>
            """.trimIndent()
        )
    }

    fun testImportMatchingRegistry() {
        compareImports("@/components/bar", "@/lib/registry/foo/bar")
    }

    fun testImportMatchingRegistry2() {
        compareImports("@/components", "@/lib/registry/foo")
    }

    fun testImportNotMatchingRegistry() {
        compareImports("@/lib/notregistry/foo", "@/lib/notregistry/foo")
    }

    fun testImportMatchingRegistryWithUI() {
        useNotNullUI = true
        compareImports("@/ui", "@/lib/registry/foo/ui")
    }

    fun testImportNotMatchingRegistryWithUI() {
        useNotNullUI = true
        compareImports("@/lib/registry/foo", "@/lib/registry/foo")
    }

    fun testImportMatchingUI() {
        compareImports("@/lib/utilsReplaced", "@/lib/utils")
    }

    fun testImportNotMatchingAny() {
        compareImports("./lib/foo", "./lib/foo")
    }
}
