package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.Config
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.Component
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.concurrency.runAsync
import java.net.URI
import java.nio.file.NoSuchFileException

abstract class Source<C : Config>(val project: Project, private val serializer: KSerializer<C>) {
    private val log = logger<Source<*>>()
    abstract var framework: String
    private val domain: String
        get() = URI(getLocalConfig().`$schema`).let { uri ->
            "${uri.scheme}://${uri.host}".also {
                log.debug("Parsed domain: $it")
            }
        }

    // Utility methods
    protected fun getLocalConfig(): C {
        val file = "components.json"
        return FileManager(project).getFileContentsAtPath(file)?.let {
            log.debug("Parsing config from $file")
            try {
                Json.decodeFromString(serializer, it).also { config ->
                    log.debug("Parsed config: ${config.javaClass.name}")
                }
            } catch (e: Exception) {
                log.error("Unable to parse $file", e)
                throw UnparseableConfigException(project, "Unable to parse $file", e)
            }
        } ?: throw NoSuchFileException("$file not found")
    }

    protected abstract fun usesDirectoriesForComponents(): Boolean

    protected abstract fun resolveAlias(alias: String): String

    protected fun cleanAlias(alias: String): String = if (alias.startsWith("\$")) {
        "\\$alias" // fixes Kotlin silently crashing when the replacement starts with $ with a regex
    } else alias

    protected open fun adaptFileExtensionToConfig(extension: String): String = extension

    protected abstract fun adaptFileToConfig(contents: String): String

    private fun fetchComponent(componentName: String): ComponentWithContents {
        val style = getLocalConfig().style
        val response = RequestSender.sendRequest("$domain/registry/styles/$style/$componentName.json")
        return response.ok { Json.decodeFromString(it.body) } ?: throw Exception("Component not found").also {
            log.error("Unable to fetch component $componentName", it)
        }
    }

    protected fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        return RequestSender.sendRequest("$domain/registry/colors/$baseColor.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found").also {
            log.error("Unable to fetch colors", it)
        }
    }

    protected open fun getRegistryDependencies(component: ComponentWithContents): List<ComponentWithContents> {
        return component.registryDependencies.map { registryDependency ->
            val dependency = fetchComponent(registryDependency)
            listOf(dependency, *getRegistryDependencies(dependency).toTypedArray())
        }.flatten()
    }

    // Public methods
    open fun fetchAllComponents(): List<ISPComponent> {
        return RequestSender.sendRequest("$domain/registry/index.json").ok {
            Json.decodeFromString<List<Component>>(it.body)
        }?.map { ISPComponent(it.name) }?.also {
            log.info("Fetched ${it.size} remote components: ${it.joinToString(", ") { component -> component.name }}")
        } ?: emptyList<ISPComponent>().also {
            log.error("Unable to fetch remote components")
        }
    }

    open fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            "${resolveAlias(getLocalConfig().aliases.components)}/ui"
        )?.children?.map { file ->
            if (file.isDirectory) file.name else file.name.substringBeforeLast(".")
        }?.sorted()?.also {
            log.info("Fetched ${it.size} installed components: ${it.joinToString(", ")}")
        } ?: emptyList<String>().also {
            log.error("Unable to fetch installed components")
        }
    }

    open fun addComponent(componentName: String) {
        // Install component
        val component = fetchComponent(componentName)
        val installedComponents = getInstalledComponents()
        log.debug("Installing ${component.name} (installed: ${installedComponents.joinToString(", ")})")
        setOf(component, *getRegistryDependencies(component).filter {
            !installedComponents.contains(it.name)
        }.toTypedArray<ComponentWithContents>()).also {
            log.debug("Installing ${it.size} components: ${it.joinToString(", ") { component -> component.name }}")
        }.forEach { downloadedComponent ->
            downloadedComponent.files.forEach { file ->
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    adaptFileExtensionToConfig(file.name),
                    FileTypeManager.getInstance().getFileTypeByExtension(
                        adaptFileExtensionToConfig(file.name).substringAfterLast('.')
                    ),
                    adaptFileToConfig(file.content)
                )
                val path = "${resolveAlias(getLocalConfig().aliases.components)}/${component.type.substringAfterLast(":")}" + if (usesDirectoriesForComponents()) {
                    "/${downloadedComponent.name}"
                } else ""
                FileManager(project).saveFileAtPath(psiFile, path)
            }
        }

        // Install dependencies
        val depsManager = DependencyManager(project)
        val depsToInstall = component.dependencies.filter { dependency ->
            !depsManager.isDependencyInstalled(dependency)
        }
        if (depsToInstall.isEmpty()) return
        log.debug("Installing ${depsToInstall.size} dependencies: ${depsToInstall.joinToString(", ") { dependency -> dependency }}")
        val dependenciesList = with(depsToInstall) {
            if (size == 1) first() else {
                "${dropLast(1).joinToString(", ")} and ${last()}"
            }
        }
        val notifManager = NotificationManager(project)
        notifManager.sendNotification(
            "Installed ${component.name}",
            "${component.name} requires $dependenciesList to be installed."
        ) { notif ->
            mapOf(
                "Install" to DependencyManager.InstallationType.PROD,
                "Install as dev" to DependencyManager.InstallationType.DEV
            ).map { (label, installType) ->
                NotificationAction.createSimple(label) {
                    runAsync {
                        depsManager.installDependencies(depsToInstall, installType)
                    }.then {
                        notifManager.sendNotificationAndHide(
                            "Installed $dependenciesList",
                            "Installed $dependenciesList for ${component.name}.",
                        )
                    }
                    notif.expire()
                }
            }
        }
    }

    open fun isComponentUpToDate(componentName: String): Boolean {
        val remoteComponent = fetchComponent(componentName)
        return remoteComponent.files.all { file ->
            (FileManager(project).getFileContentsAtPath(
                "${resolveAlias(getLocalConfig().aliases.components)}/${remoteComponent.type.substringAfterLast(":")}${if (usesDirectoriesForComponents()) {
                    "/${remoteComponent.name}"
                } else ""}/${file.name}"
            ) == adaptFileToConfig(file.content)).also {
                log.debug("File ${file.name} for ${remoteComponent.name} is ${if (it) "" else "NOT "}up to date")
            }
        }
    }

    open fun removeComponent(componentName: String) {
        val remoteComponent = fetchComponent(componentName)
        val componentsDir = "${resolveAlias(getLocalConfig().aliases.components)}/${remoteComponent.type.substringAfterLast(":")}"
        if (usesDirectoriesForComponents()) {
            FileManager(project).deleteFileAtPath("$componentsDir/${remoteComponent.name}")
        } else {
            remoteComponent.files.forEach { file ->
                FileManager(project).deleteFileAtPath("$componentsDir/${file.name}")
            }
        }
    }
}
