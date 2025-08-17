package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidUIConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SolidUIReplacementsTests : ReplacementsTests() {

    class SolidUISourceStub(project: Project) : SolidUISource(project) {
        override fun getLocalConfig() = SolidUIConfig(
            tsx = true,
            tailwind = SolidUIConfig.Tailwind(
                config = "tailwind.config.cjs",
                css = "src/app.css"
            ),
            aliases = SolidUIConfig.Aliases(
                components = "@/*",
                utils = ""
            )
        )

        public override fun adaptFileToConfig(file: PsiFile) {
            super.adaptFileToConfig(file)
        }
    }

    private val compareImports by lazy {
        compareImportsBuilder(
            SolidUISourceStub(project)::adaptFileToConfig,
            "tsx",
            """
            import { foo } from "{{import}}";
            """.trimIndent()
        )
    }

    fun testImportMatchingRegistry() {
        compareImports("@/components/ui", "~/registry/ui")
    }

    fun testImportMatchingRegistry2() {
        compareImports("@/components/ui/foo", "~/registry/ui/foo")
    }

    fun testImportNotMatchingRegistry() {
        compareImports("~/registry/bar", "~/registry/bar")
    }

    fun testImportMatchingUtils() {
        compareImports("@/lib/utils", "~/lib/utils")
    }

    fun testImportMatchingUtils2() {
        compareImports("@/lib/utils/foo", "~/lib/utils/foo")
    }

    fun testImportNotMatchingUtils() {
        compareImports("~/lib/bar", "~/lib/bar")
    }
}
