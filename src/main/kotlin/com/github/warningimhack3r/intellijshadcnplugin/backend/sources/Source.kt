package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.Config
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.Component
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationAction
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
    abstract var framework: String
    private val domain: String
        get() = URI(getLocalConfig().`$schema`).let { uri ->
            "${uri.scheme}://${uri.host}"
        }

    // Utility methods
    protected fun getLocalConfig(): C {
        return FileManager(project).getFileContentsAtPath("components.json")?.let {
            try {
                Json.decodeFromString(serializer, it)
            } catch (e: Exception) {
                throw UnparseableConfigException(project, "Unable to parse components.json", e)
            }
        } ?: throw NoSuchFileException("components.json not found")
    }

    protected abstract fun resolveAlias(alias: String): String

    protected fun cleanAlias(alias: String): String = if (alias.startsWith("\$")) {
        "\\$alias" // fixes Kotlin silently crashing when the replacement starts with $ with a regex
    } else alias

    protected open fun adaptFileExtensionToConfig(extension: String): String = extension

    protected abstract fun adaptFileToConfig(contents: String): String

    private fun fetchComponent(componentName: String): ComponentWithContents {
        val style = getLocalConfig().style
        val response = RequestSender.sendRequest("$domain/registry/styles/$style/$componentName.json")
        return response.ok { Json.decodeFromString(it.body) } ?: throw Exception("Component not found")
    }

    protected fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        val response = RequestSender.sendRequest("$domain/registry/colors/$baseColor.json")
        return response.ok { Json.parseToJsonElement(it.body) } ?: throw Exception("Colors not found")
    }

    // Public methods
    open fun fetchAllComponents(): List<ISPComponent> {
        val response = RequestSender.sendRequest("$domain/registry/index.json")
        return response.ok {
            Json.decodeFromString<List<Component>>(it.body)
        }?.map { ISPComponent(it.name) } ?: emptyList()
    }

    open fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            "${resolveAlias(getLocalConfig().aliases.components)}/ui"
        )?.children?.map { it.name }?.sorted() ?: emptyList()
    }

    open fun addComponent(componentName: String) {
        val installedComponents = getInstalledComponents()
        fun getRegistryDependencies(component: ComponentWithContents): List<ComponentWithContents> {
            return component.registryDependencies.filter {
                !installedComponents.contains(it)
            }.map { registryDependency ->
                val dependency = fetchComponent(registryDependency)
                listOf(dependency, *getRegistryDependencies(dependency).toTypedArray())
            }.flatten()
        }

        // Install component
        val component = fetchComponent(componentName)
        val components = setOf(component, *getRegistryDependencies(fetchComponent(componentName)).toTypedArray())
        val config = getLocalConfig()
        components.forEach { downloadedComponent ->
            downloadedComponent.files.forEach { file ->
                val path = "${resolveAlias(config.aliases.components)}/${component.type.substringAfterLast(":")}/${downloadedComponent.name}"
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    adaptFileExtensionToConfig(file.name),
                    FileTypeManager.getInstance().getFileTypeByExtension(
                        adaptFileExtensionToConfig(file.name).substringAfterLast('.')
                    ),
                    adaptFileToConfig(file.content)
                )
                FileManager(project).saveFileAtPath(psiFile, path)
            }
        }

        // Install dependencies
        val depsManager = DependencyManager(project)
        val depsToInstall = component.dependencies.filter { dependency ->
            !depsManager.isDependencyInstalled(dependency)
        }
        if (depsToInstall.isEmpty()) return
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
        val config = getLocalConfig()
        val remoteComponent = fetchComponent(componentName)
        return remoteComponent.files.all { file ->
            FileManager(project).getFileContentsAtPath(
                "${resolveAlias(config.aliases.components)}/${remoteComponent.type.substringAfterLast(":")}/${remoteComponent.name}/${file.name}"
            ) == adaptFileToConfig(file.content)
        }
    }

    open fun removeComponent(componentName: String) {
        val remoteComponent = fetchComponent(componentName)
        FileManager(project).deleteFileAtPath(
            "${resolveAlias(getLocalConfig().aliases.components)}/${remoteComponent.type.substringAfterLast(":")}/${remoteComponent.name}"
        )
    }
}
