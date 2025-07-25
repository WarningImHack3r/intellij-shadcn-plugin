package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.*
import java.nio.file.NoSuchFileException

open class SvelteSource(project: Project) : Source<SvelteConfig>(project, SvelteConfig.serializer()) {
    companion object {
        private val log = logger<SvelteSource>()
    }

    override var framework = "Svelte"

    override fun getURLPathForComponent(componentName: String) =
        "registry/styles/${getLocalConfig().style}/$componentName.json"

    override fun getLocalPathForComponents() = getLocalConfig().aliases.components

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }
        val fileManager = FileManager.getInstance(project)
        val usesKit = DependencyManager.getInstance(project).isDependencyInstalled("@sveltejs/kit")
        val configFileName = if (usesKit) {
            ".svelte-kit/tsconfig.json"
        } else if (getLocalConfig().typescript) "tsconfig.json" else "jsconfig.json"
        var tsConfig = fileManager.getFileContentsAtPath(configFileName)
        if (tsConfig == null) {
            if (!usesKit) throw NoSuchFileException("Cannot get $configFileName")
            val res = ShellRunner.getInstance(project).execute(arrayOf("npx", "svelte-kit", "sync"))
            if (res == null) {
                NotificationManager(project).sendNotification(
                    "Failed to generate $configFileName",
                    "Please run <code>npx svelte-kit sync</code> in your project directory to generate the file and try again.",
                    NotificationType.ERROR
                )
                throw NoSuchFileException("Cannot get or generate $configFileName")
            }
            Thread.sleep(500) // wait for the sync to create the files
            tsConfig =
                fileManager.getFileContentsAtPath(configFileName)
                    ?: throw NoSuchFileException("Cannot get $configFileName after sync")
        }
        val aliasPath = parseTsConfig(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get(alias.substringBefore("/"))
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return "${aliasPath.replace(Regex("^\\.+/"), "")}/${alias.substringAfter("/")}".also {
            log.debug("Resolved alias $alias to $it")
        }
    }

    override fun fetchComponent(componentName: String): ComponentWithContents {
        val config = getLocalConfig()
        return if (config.typescript) {
            super.fetchComponent(componentName)
        } else {
            RequestSender.sendRequest("$domain/registry/styles/${config.style}-js/$componentName.json")
                .ok {
                    val json = Json { ignoreUnknownKeys = true }
                    json.decodeFromString(ComponentWithContents.serializer(), it.body)
                } ?: throw Exception("Component $componentName not found")
        }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (!getLocalConfig().typescript) {
            extension.replace(
                Regex("\\.ts$"),
                ".js"
            )
        } else extension
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()
        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports { `package` ->
            `package`
                .replace(Regex("^\\\$lib/registry/[^/]+"), escapeRegexValue(config.aliases.components))
                .replace("\$lib/utils", config.aliases.utils)
        }
    }

    override fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        return RequestSender.sendRequest("$domain/registry/colors/$baseColor.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found")
    }
}
