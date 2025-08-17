package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonArray
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidUIConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.nio.file.NoSuchFileException

open class SolidUISource(project: Project) : Source<SolidUIConfig>(project, SolidUIConfig.serializer()) {
    companion object {
        private val log = logger<SolidUISource>()
        private var isJsUnsupportedNotified = false
    }

    override var framework = "Solid"

    override val configFile = "ui.config.json"

    override fun getURLPathForRoot() = "registry/index.json"

    override fun getURLPathForComponent(componentName: String) = "registry/ui/$componentName.json"

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
            .asJsonObject?.get("compilerOptions")
            ?.asJsonObject?.get("paths")
            ?.asJsonObject?.get("${alias.substringBefore("/")}/*")
            ?.asJsonArray?.get(0)
            ?.asJsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return aliasPath.replace(Regex("^\\.+/"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/")).also {
                log.debug("Resolved alias $alias to $it")
            }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (getLocalConfig().tsx) extension else {
            extension
                .replace(Regex("\\.tsx$"), ".ts")
                .replace(Regex("\\.jsx$"), ".js")
        }
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
        importsPackagesReplacementVisitor.replaceImports replacer@{ `package` ->
            return@replacer `package`
                .replace("~/registry/ui", config.aliases.components)
                .replace("~/lib/utils", config.aliases.utils)
        }

        val replacementVisitor = JSXClassReplacementVisitor(project)
        runReadAction { file.accept(replacementVisitor) }
        replacementVisitor.replaceClasses replacer@{ `class` ->
            val modifier = if (`class`.contains(":")) `class`.substringBeforeLast(":") + ":" else ""
            val className = `class`.substringAfterLast(":")
            val twPrefix = config.tailwind.prefix
            return@replacer "$modifier$twPrefix$className"
        }
    }
}
