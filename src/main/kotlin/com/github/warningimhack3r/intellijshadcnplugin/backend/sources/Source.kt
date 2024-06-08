package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.Config
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.Component
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.concurrency.runAsync
import java.net.URI
import java.nio.file.NoSuchFileException

abstract class Source<C : Config>(val project: Project, private val serializer: KSerializer<C>) {
    private val log = logger<Source<C>>()
    private var config: C? = null

    protected val domain: String
        get() = URI(getLocalConfig().`$schema`).let { uri ->
            "${uri.scheme}://${uri.host}".also {
                log.debug("Parsed domain: $it")
            }
        }
    protected val tsConfigJson = Json {
        isLenient = true
        allowTrailingCommas = true
        allowComments = true
    }

    protected open val configFile = "components.json"

    abstract val framework: String

    // Paths
    protected abstract fun getURLPathForComponent(componentName: String): String

    protected abstract fun getLocalPathForComponents(): String

    // Utility methods
    /**
     * Gets the local shadcn config, specified by the [configFile] name.
     * If the config is already cached, it will return the cached config;
     * otherwise, it will try to find it in the project FS, read it and
     * parse it before caching it.
     *
     * Note: This method is `open` **ONLY** so that it can be overridden in
     * tests. It should **NOT** be overridden in regular implementations.
     *
     * @throws NoSuchFileException If the config is not found
     * @throws UnparseableConfigException If the config is found but cannot be parsed
     *
     * @return The local shadcn config
     */
    protected open fun getLocalConfig(): C {
        return config?.also {
            log.debug("Returning cached config")
        } ?: FileManager(project).getFileContentsAtPath(configFile)?.let {
            log.debug("Parsing config from $configFile")
            try {
                Json.decodeFromString(serializer, it).also {
                    log.debug("Parsed config")
                }
            } catch (e: Exception) {
                throw UnparseableConfigException(project, configFile, e)
            }
        }?.also {
            if (config == null) {
                log.debug("Caching config")
                config = it
            }
        } ?: throw NoSuchFileException("$configFile not found")
    }

    protected abstract fun usesDirectoriesForComponents(): Boolean

    protected abstract fun resolveAlias(alias: String): String

    /**
     * Escapes the value if it starts with a $. MUST be used when [String.replace]
     * is used with a [Regex] as a first argument and when the second may start with a $.
     * Otherwise, Kotlin will silently fail.
     *
     * @param value The value to escape
     * @return The value, escaped if necessary
     */
    protected fun escapeRegexValue(value: String) = if (value.startsWith("\$")) {
        "\\$value" // fixes Kotlin silently failing when the replacement starts with $ with a regex
    } else value

    protected open fun adaptFileExtensionToConfig(extension: String): String = extension

    protected abstract fun adaptFileToConfig(file: PsiFile)

    protected open fun fetchComponent(componentName: String): ComponentWithContents {
        return RequestSender.sendRequest("$domain/${getURLPathForComponent(componentName)}")
            .ok { Json.decodeFromString(it.body) } ?: throw Exception("Component $componentName not found")
    }

    protected open fun fetchColors(): JsonElement {
        throw NoSuchMethodException("Not implemented")
    }

    protected open fun getRegistryDependencies(component: ComponentWithContents): List<ComponentWithContents> {
        return component.registryDependencies.map { registryDependency ->
            val dependency = fetchComponent(registryDependency)
            listOf(dependency, *getRegistryDependencies(dependency).toTypedArray())
        }.flatten()
    }

    // Public methods
    open fun fetchAllComponents(): List<Component> {
        return RequestSender.sendRequest("$domain/registry/index.json").ok {
            Json.decodeFromString<List<Component>>(it.body)
        }?.also {
            log.info("Fetched ${it.size} remote components: ${it.joinToString(", ") { component -> component.name }}")
        } ?: emptyList<Component>().also {
            log.warn("Unable to fetch remote components")
        }
    }

    open fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            "${resolveAlias(getLocalPathForComponents())}/ui"
        )?.children?.map { file ->
            if (file.isDirectory) file.name else file.name.substringBeforeLast(".")
        }?.sorted()?.also {
            log.info("Found ${it.size} installed components: ${it.joinToString(", ")}")
        } ?: emptyList<String>().also {
            log.warn("Unable to find installed components")
        }
    }

    open fun addComponent(componentName: String) {
        val componentsPath = resolveAlias(getLocalPathForComponents())
        // Install component
        val component = fetchComponent(componentName)
        val installedComponents = getInstalledComponents()
        val fileManager = FileManager(project)
        val notifManager = NotificationManager(project)
        log.debug("Installing ${component.name} (installed: ${installedComponents.joinToString(", ")})")
        setOf(component, *getRegistryDependencies(component).filter {
            !installedComponents.contains(it.name)
        }.toTypedArray<ComponentWithContents>()).also {
            log.debug("Installing ${it.size} components: ${it.joinToString(", ") { component -> component.name }}")
        }.forEach { downloadedComponent ->
            val path =
                "${componentsPath}/${component.type.substringAfterLast(":")}" + if (usesDirectoriesForComponents()) {
                    "/${downloadedComponent.name}"
                } else ""
            // Check for deprecated components
            if (usesDirectoriesForComponents()) {
                val remotelyDeletedFiles = fileManager.getFileAtPath(path)?.children?.filter { file ->
                    downloadedComponent.files.none { it.name == file.name }
                } ?: emptyList()
                if (remotelyDeletedFiles.isNotEmpty()) {
                    val multipleFiles = remotelyDeletedFiles.size > 1
                    notifManager.sendNotification(
                        "Deprecated component file${if (multipleFiles) "s" else ""} in ${downloadedComponent.name}",
                        "The following file${if (multipleFiles) "s are" else " is"} no longer part of ${downloadedComponent.name}: ${
                            remotelyDeletedFiles.joinToString(", ") { file ->
                                file.name
                            }
                        }. Do you want to remove ${if (multipleFiles) "them" else "it"}?"
                    ) { notification ->
                        listOf(
                            NotificationAction.createSimple("Remove " + if (multipleFiles) "them" else "it") {
                                remotelyDeletedFiles.forEach { file ->
                                    fileManager.deleteFile(file)
                                }
                                log.info(
                                    "Removed deprecated file${if (multipleFiles) "s" else ""} from ${downloadedComponent.name} (${
                                        downloadedComponent.files.joinToString(", ") { file ->
                                            file.name
                                        }
                                    }): ${
                                        remotelyDeletedFiles.joinToString(", ") { file ->
                                            file.name
                                        }
                                    }"
                                )
                                notification.expire()
                            },
                            NotificationAction.createSimple("Keep " + if (multipleFiles) "them" else "it") {
                                notification.expire()
                            }
                        )
                    }
                }
            }
            downloadedComponent.files.forEach { file ->
                val psiFile = PsiHelper.createPsiFile(
                    project, adaptFileExtensionToConfig(file.name), file.content
                )
                adaptFileToConfig(psiFile)
                fileManager.saveFileAtPath(psiFile, path)
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
        val componentPath =
            "${resolveAlias(getLocalPathForComponents())}/${remoteComponent.type.substringAfterLast(":")}${
                if (usesDirectoriesForComponents()) {
                    "/${remoteComponent.name}"
                } else ""
            }"
        val fileManager = FileManager(project)
        return remoteComponent.files.all { file ->
            val psiFile = PsiHelper.createPsiFile(
                project, adaptFileExtensionToConfig(file.name), file.content
            )
            adaptFileToConfig(psiFile)
            (fileManager.getFileContentsAtPath("$componentPath/${file.name}") == runReadAction {
                psiFile.text
            }).also {
                log.debug("File ${file.name} for ${remoteComponent.name} is ${if (it) "" else "NOT "}up to date")
            }
        }
    }

    open fun removeComponent(componentName: String) {
        val remoteComponent = fetchComponent(componentName)
        val componentsDir =
            "${resolveAlias(getLocalPathForComponents())}/${remoteComponent.type.substringAfterLast(":")}"
        if (usesDirectoriesForComponents()) {
            FileManager(project).deleteFileAtPath("$componentsDir/${remoteComponent.name}")
        } else {
            remoteComponent.files.forEach { file ->
                FileManager(project).deleteFileAtPath("$componentsDir/${file.name}")
            }
        }
        // Remove dependencies no longer needed by any component
        val remoteComponents = fetchAllComponents()
        val allPossiblyNeededDependencies = remoteComponents.map { it.dependencies }.flatten().toSet()
        val currentlyNeededDependencies = getInstalledComponents().map { component ->
            remoteComponents.find { it.name == component }?.dependencies ?: emptyList()
        }.flatten().toSet()
        val uselessDependencies = DependencyManager(project).getInstalledDependencies().filter { dependency ->
            dependency in allPossiblyNeededDependencies && dependency !in currentlyNeededDependencies
        }
        if (uselessDependencies.isNotEmpty()) {
            val multipleDependencies = uselessDependencies.size > 1
            val notifManager = NotificationManager(project)
            notifManager.sendNotification(
                "Unused dependenc${if (multipleDependencies) "ies" else "y"} found",
                "The following dependenc${if (multipleDependencies) "ies are" else "y is"} no longer needed by any component: ${
                    uselessDependencies.joinToString(", ") {
                        it
                    }
                }. Do you want to remove ${if (multipleDependencies) "them" else "it"}?"
            ) { notif ->
                listOf(
                    NotificationAction.createSimple("Remove") {
                        runAsync {
                            DependencyManager(project).uninstallDependencies(uselessDependencies)
                        }.then {
                            notifManager.sendNotificationAndHide(
                                "Removed dependenc${if (multipleDependencies) "ies" else "y"}",
                                "Removed ${uselessDependencies.joinToString(", ") { it }}."
                            )
                        }
                        notif.expire()
                    }
                )
            }
        }
    }
}
