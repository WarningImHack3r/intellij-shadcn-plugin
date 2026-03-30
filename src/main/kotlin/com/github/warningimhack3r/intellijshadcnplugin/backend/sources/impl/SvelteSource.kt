package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonArray
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfigDeserializer
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfigTsBoolean
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfigTsObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.NoSuchFileException

open class SvelteSource(project: Project) : Source<SvelteConfig>(project, SvelteConfigDeserializer) {
    companion object {
        private val log = logger<SvelteSource>()
        private var isJsUnsupportedNotified = false
    }

    override val domain: String
        get() = getLocalConfig().registry

    override var framework = "Svelte"

    override fun getURLPathForRoot() = "index.json"

    override fun getURLPathForComponent(componentName: String) = "$componentName.json"

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }
        val fileManager = FileManager.getInstance(project)
        val usesKit = DependencyManager.getInstance(project).isDependencyInstalled("@sveltejs/kit")
        val configFileName = if (usesKit) ".svelte-kit/tsconfig.json" else {
            with(getLocalConfig()) {
                when (this) {
                    is SvelteConfigTsObject -> typescript.config?.takeIf { it.isNotBlank() } ?: "tsconfig.json"
                    is SvelteConfigTsBoolean -> if (typescript) "tsconfig.json" else "jsconfig.json"
                }
            }
        }
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
        // Handle scoped package aliases (e.g. @repo/ui/components).
        // For @-prefixed aliases, the package name includes a slash (@scope/name),
        // so we split into at most 3 parts to correctly separate the prefix from the rest.
        val (aliasPrefix, suffix) = if (alias.startsWith("@")) {
            val parts = alias.split("/", limit = 3)
            val prefix = if (parts.size >= 2) "${parts[0]}/${parts[1]}" else alias
            val rest = if (parts.size >= 3) parts[2] else ""
            prefix to rest
        } else {
            alias.substringBefore("/") to alias.substringAfter("/", "")
        }

        val paths = parseTsConfig(tsConfig)
            .asJsonObject?.get("compilerOptions")
            ?.asJsonObject?.get("paths")

        val aliasPath = (paths?.asJsonObject?.get(aliasPrefix)
            ?: paths?.asJsonObject?.get("$aliasPrefix/*"))
            ?.asJsonArray?.get(0)
            ?.asJsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        val normalized = aliasPath.replace(Regex("^\\.+/"), "").removeSuffix("/*")
        val resolved = if (suffix.isEmpty()) normalized else "$normalized/$suffix"
        return resolved.also { log.debug("Resolved alias $alias to $it") }
    }

    private fun wantsTypescript(config: SvelteConfig) = with(config) {
        when (this) {
            is SvelteConfigTsObject -> true
            is SvelteConfigTsBoolean -> typescript
        }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (wantsTypescript(getLocalConfig())) extension else {
            extension.replace(
                Regex("\\.ts$"),
                ".js"
            )
        }
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()
        if (!wantsTypescript(config)) {
            if (!isJsUnsupportedNotified) {
                NotificationManager(project).sendNotification(
                    "TypeScript option for Svelte",
                    "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                    NotificationType.WARNING
                )
                isJsUnsupportedNotified = true
            }
            // TODO: detype Svelte file
        }

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports { `package` ->
            var renamedPackage = `package`
            SvelteConfig.Aliases.Alias.entries.forEach { alias ->
                renamedPackage = renamedPackage.replace(
                    alias.placeholder,
                    config.aliases.fromRaw(alias.slotName).orEmpty()
                )
            }
            renamedPackage
        }
    }

    override fun fetchColors(): JsonElement {
        val config = getLocalConfig()
        return RequestSender.sendRequest("${config.registry}/colors/${config.tailwind.baseColor}.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found")
    }
}
