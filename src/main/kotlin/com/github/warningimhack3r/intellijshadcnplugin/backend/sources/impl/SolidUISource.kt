package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidUIConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

open class SolidUISource(project: Project) : Source<SolidUIConfig>(project, SolidUIConfig.serializer()) {
    companion object {
        private val log = logger<SolidUISource>()
        private var isJsUnsupportedNotified = false
    }

    override var framework = "Solid"

    override val configFile = "ui.config.json"

    override fun getURLPathForComponent(componentName: String) = "registry/ui/$componentName.json"

    override fun getLocalPathForComponents() = getLocalConfig().componentDir

    override fun usesDirectoriesForComponents() = false

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }
        val configFile = if (getLocalConfig().tsx) "tsconfig.json" else "jsconfig.json"
        val tsConfig = FileManager.getInstance(project).getFileContentsAtPath(configFile)
            ?: throw NoSuchFileException("$configFile not found")
        val aliasPath = parseTsConfig(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get("${alias.substringBefore("/")}/*")
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return aliasPath.replace(Regex("^\\.+/"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/")).also {
                log.debug("Resolved alias $alias to $it")
            }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (!getLocalConfig().tsx) {
            extension
                .replace(Regex("\\.tsx$"), ".ts")
                .replace(Regex("\\.jsx$"), ".js")
        } else extension
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()

        if (!config.tsx) {
            if (!isJsUnsupportedNotified) {
                NotificationManager(project).sendNotification(
                    "TypeScript option for Solid",
                    "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                    NotificationType.WARNING
                )
                isJsUnsupportedNotified = true
            }
            // TODO: detype Solid file
        }

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        val pathAlias = config.aliases.path.replace("/*", "")
        importsPackagesReplacementVisitor.replaceImports replacer@{ `package` ->
            return@replacer `package`
                .replace("~/registry/ui", "$pathAlias/components/ui")
                .replace("~/lib/utils", "$pathAlias/lib/utils")
        }
    }
}
