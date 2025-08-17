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
        compareImports("@/components/bar", "@/registry/foo/bar")
    }

    fun testImportMatchingRegistry2() {
        compareImports("@/components", "@/registry/foo")
    }

    fun testImportNotMatchingRegistry() {
        compareImports("@/notregistry/foo", "@/notregistry/foo")
    }

    fun testImportMatchingRegistryWithUI() {
        useNotNullUI = true
        compareImports("@/ui", "@/registry/foo/ui")
    }

    fun testImportMatchingRegistryWithUIComponents() {
        useNotNullUI = true
        compareImports("@/components", "@/registry/foo")
    }

    fun testImportMatchingUI() {
        compareImports("@/lib/utilsReplaced", "@/lib/utils")
    }

    fun testImportNotMatchingAny() {
        compareImports("./lib/foo", "./lib/foo")
    }
}
