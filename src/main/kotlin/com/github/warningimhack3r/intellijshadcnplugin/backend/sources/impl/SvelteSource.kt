package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class SvelteSource(project: Project) : Source<SvelteConfig>(project, SvelteConfig.serializer()) {
    private val log = logger<SvelteSource>()
    override var framework = "Svelte"

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias.also {
            log.debug("Alias $alias does not start with $ or @, returning it as-is")
        }
        val configFile = ".svelte-kit/tsconfig.json"
        val fileManager = FileManager(project)
        var tsConfig = fileManager.getFileContentsAtPath(configFile)
        if (tsConfig == null) {
            val res = ShellRunner(project).execute(arrayOf("npx", "svelte-kit", "sync"))
            if (res == null) {
                NotificationManager(project).sendNotification(
                    "Failed to generate $configFile",
                    "Please run <code>npx svelte-kit sync</code> in your project directory to generate the file and try again.",
                    NotificationType.ERROR
                )
                log.error("Failed to generate $configFile, sent notification and throwing exception")
                throw NoSuchFileException("Cannot get or generate $configFile")
            }
            Thread.sleep(250) // wait for the sync to create the files
            tsConfig = fileManager.getFileContentsAtPath(configFile) ?: throw NoSuchFileException("Cannot get $configFile").also {
                log.error("Failed to get $configFile once again, throwing exception")
            }
        }
        val aliasPath = Json.parseToJsonElement(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get(alias.substringBefore("/"))
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias").also {
                log.error("Failed to find alias $alias in $tsConfig, throwing exception")
            }
        return "${aliasPath.replace(Regex("^\\.+/"), "")}/${alias.substringAfter("/")}".also {
            log.debug("Resolved alias $alias to $it")
        }
    }

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        return contents.replace(
            Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
        ).replace(
            "\$lib/utils", config.aliases.utils
        )
    }
}
