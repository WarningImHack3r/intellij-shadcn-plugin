package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.ReactConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ReactReplacementsTests : ReplacementsTests() {
    private var useNotNullUI = false

    inner class ReactSourceStub(project: Project) : ReactSource(project) {
        override fun getLocalConfig() = ReactConfig(
            style = "default",
            rsc = true,
            tsx = true,
            tailwind = ReactConfig.Tailwind(
                config = "tailwind.config.js",
                css = "app/globals.css",
                baseColor = "slate",
                cssVariables = true,
                prefix = ""
            ),
            aliases = ReactConfig.Aliases(
                components = "@/components",
                utils = "@/lib/utilsReplaced",
                ui = if (useNotNullUI) "@/ui" else null,
                lib = "@/lib",
                hooks = "@/hooks"
            ),
            iconLibrary = "lucide"
        )

        public override fun adaptFileToConfig(file: PsiFile) {
            super.adaptFileToConfig(file)
        }
    }

    private val compareImports by lazy {
        compareImportsBuilder(
            ReactSourceStub(project)::adaptFileToConfig,
            "tsx",
            """
            import { foo } from "{{import}}";
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
        compareImports("@/lib/notregistry/foo", "@/lib/notregistry/foo")
    }

    fun testImportMatchingRegistryWithUI() {
        useNotNullUI = true
        compareImports("@/ui", "@/registry/foo/ui")
    }

    fun testImportNotMatchingRegistryWithUI() {
        useNotNullUI = true
        compareImports("@/registry/foo", "@/registry/foo")
    }

    fun testImportMatchingUI() {
        compareImports("@/lib/utilsReplaced", "@/lib/utils")
    }

    fun testImportNotMatchingAny() {
        compareImports("./lib/foo", "./lib/foo")
    }
}
