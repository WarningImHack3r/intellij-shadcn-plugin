package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class SvelteSource(project: Project) : Source<SvelteConfig>(project, SvelteConfig.serializer()) {
    companion object {
        private val log = logger<SvelteSource>()
    }
    override var framework = "Svelte"

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias.also {
            log.debug("Alias $alias does not start with $ or @, returning it as-is")
        }
        val usesKit = DependencyManager(project).isDependencyInstalled("@sveltejs/kit")
        val tsConfigName = if (getLocalConfig().typescript) "tsconfig.json" else "jsconfig.json"
        val configFile = if (usesKit) ".svelte-kit/$tsConfigName" else tsConfigName
        val fileManager = FileManager(project)
        var tsConfig = fileManager.getFileContentsAtPath(configFile)
        if (tsConfig == null) {
            if (!usesKit) throw NoSuchFileException("Cannot get $configFile")
            val res = ShellRunner(project).execute(arrayOf("npx", "svelte-kit", "sync"))
            if (res == null) {
                NotificationManager(project).sendNotification(
                    "Failed to generate $configFile",
                    "Please run <code>npx svelte-kit sync</code> in your project directory to generate the file and try again.",
                    NotificationType.ERROR
                )
                throw NoSuchFileException("Cannot get or generate $configFile")
            }
            Thread.sleep(250) // wait for the sync to create the files
            tsConfig =
                fileManager.getFileContentsAtPath(configFile) ?: throw NoSuchFileException("Cannot get $configFile")
        }
        val aliasPath = Json.parseToJsonElement(tsConfig)
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
                .ok { Json.decodeFromString(it.body) } ?: throw Exception("Component $componentName not found")
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

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        return contents.replace(
            Regex("([\"'])[^\r\n/]+/registry/[^/]+"), "$1${cleanAlias(config.aliases.components)}"
        ).replace(
            "\$lib/utils", config.aliases.utils
        )
    }
}
