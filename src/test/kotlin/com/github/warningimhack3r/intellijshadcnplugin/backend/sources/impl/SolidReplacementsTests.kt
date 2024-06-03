package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidConfig
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SolidReplacementsTests : ReplacementsTests() {

    class SolidSourceStub(project: Project) : SolidSource(project) {
        override fun getLocalConfig() = SolidConfig(
            tailwind = SolidConfig.Tailwind(
                config = "tailwind.config.cjs",
                css = "src/root.css",
                baseColor = "slate",
                cssVariables = true
            ),
            aliases = SolidConfig.Aliases(
                components = "@/components",
                utils = "@/lib/utils"
            )
        )

        public override fun adaptFileToConfig(file: PsiFile) {
            super.adaptFileToConfig(file)
        }
    }

    private val compareImports by lazy {
        compareImportsBuilder(
            SolidSourceStub(project)::adaptFileToConfig,
            "tsx",
            """
            import { foo } from "{{import}}";
            """.trimIndent()
        )
    }

    fun testImportMatchingLibsCn() {
        compareImports("@/lib/utils", "@/libs/cn")
    }

    fun testImportNotMatchingLibsCn() {
        compareImports("@/libs/cn/foo", "@/libs/cn/foo")
    }

    fun testImportNotMatchingLibsCn2() {
        compareImports("@/lib/cn", "@/lib/cn")
    }
}
