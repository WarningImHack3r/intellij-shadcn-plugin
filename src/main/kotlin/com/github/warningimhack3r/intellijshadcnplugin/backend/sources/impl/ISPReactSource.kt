package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPComponent
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPStyle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.util.applyIf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.jetbrains.concurrency.runAsync
import java.nio.file.NoSuchFileException

class ISPReactSource(private val project: Project): ISPSource {
    override var domain = "https://ui.shadcn.com"
    override var language = "React"

    private fun fetchComponent(componentName: String): ReactTypes.ComponentWithContents {
        val style = getLocalConfig().style
        val response = RequestSender.sendRequest("$domain/registry/styles/$style/$componentName.json")
        return response.body?.let { Json.decodeFromString<ReactTypes.ComponentWithContents>(it) } ?: throw Exception("Component not found")
    }

    private fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        val response = RequestSender.sendRequest("$domain/registry/colors/$baseColor.json")
        return response.body?.let { Json.parseToJsonElement(it) } ?: throw Exception("Colors not found")
    }

    private fun getLocalConfig(): ReactTypes.Config {
        return FileManager(project).getFileContentsAtPath("components.json")?.let {
            Json.decodeFromString(it)
        } ?: throw NoSuchFileException("components.json not found")
    }

    private fun getConfigFileName(fileName: String): String {
        return if (!getLocalConfig().tsx) {
            fileName
                .replace(Regex("\\.tsx$"), ".ts")
                .replace(Regex("\\.jsx$"), ".js")
        } else fileName
    }

    private fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias
        val tsConfig = FileManager(project).getFileContentsAtPath("tsconfig.json") ?: throw NoSuchFileException("tsconfig.json not found")
        val aliasPath = Json.parseToJsonElement(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get("${alias.substringBefore("/")}/*")
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias")
        return aliasPath.replace(Regex("^\\./"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/"))
    }

    private fun adaptFileContents(contents: String): String {
        fun cleanAlias(alias: String): String {
            return if (alias.startsWith("\$")) {
                "\\$alias" // fixes Kotlin silently crashing when the replacement starts with $ with a regex
            } else alias
        }

        val config = getLocalConfig()
        // Note: this condition does not replace UI paths (= $components/$ui) by the components path
        // if the UI alias is not set.
        // For me, this is a bug, but I'm following what the original code does for parity
        // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-import.ts#L10-L23).
        var newContents = if (config.aliases.ui != null) {
            contents.replace(
                Regex("@/registry/[^/]+/ui"), cleanAlias(config.aliases.ui)
            )
        } else contents.replace(
            Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
        )
        newContents = newContents.replace(
            // Note: this does not prevent additional imports other than "cn" from being replaced,
            // but I'm once again following what the original code does for parity
            // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-import.ts#L25-L35).
            Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"@/lib/utils"), config.aliases.utils
        ).applyIf(config.rsc) {
            replace(
                Regex("\"use client\";*\n"), ""
            )
        }

        /**
         * Prepends `tw-` to all Tailwind classes.
         * @param classes The classes to prefix, an unquoted string of space-separated class names.
         * @param prefix The prefix to add to each class name.
         * @return The prefixed classes.
         */
        fun prefixClasses(classes: String, prefix: String): String = classes
            .split(" ")
            .filterNot { it.isEmpty() }
            .joinToString(" ") {
                val className = it.trim().split(":")
                if (className.size == 1) {
                    "$prefix${className[0]}"
                } else {
                    "${className.dropLast(1).joinToString(":")}:$prefix${className.last()}"
                }
            }

        /**
         * Converts CSS variables to Tailwind utility classes.
         * @param classes The classes to convert, an unquoted string of space-separated class names.
         * @param lightColors The light colors map to use.
         * @param darkColors The dark colors map to use.
         * @return The converted classes.
         */
        fun variablesToUtilities(classes: String, lightColors: Map<String, String>, darkColors: Map<String, String>): String {
            // Note: this does not include `border` classes at the beginning or end of the string,
            // but I'm once again following what the original code does for parity
            // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-css-vars.ts#L142-L145).
            val newClasses = classes.replace(" border ", " border border-border ")

            val prefixesToReplace = listOf("bg-", "text-", "border-", "ring-offset-", "ring-")

            /**
             * Replaces a class with CSS variables with Tailwind utility classes.
             * @param class The class to replace.
             * @return The replaced class.
             */
            fun replaceClass(`class`: String): String {
                val prefix = prefixesToReplace.find { `class`.startsWith(it) } ?: return `class`
                val color = `class`.substringAfter(prefix)
                val lightColor = lightColors[color]
                val darkColor = darkColors[color]
                return if (lightColor != null && darkColor != null) {
                    "$prefix$lightColor dark:$prefix$darkColor"
                } else `class`
            }

            return newClasses
                .split(" ")
                .filterNot { it.isEmpty() }
                .joinToString(" ") {
                    val className = it.trim().split(":")
                    if (className.size == 1) {
                        replaceClass(className[0])
                    } else {
                        "${className.dropLast(1).joinToString(":")}:${replaceClass(className.last())}"
                    }
                }
        }

        fun handleClasses(classes: String): String {
            var newClasses = classes
            if (!config.tailwind.cssVariables) {
                val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject ?: throw Exception("Inline colors not found")
                newClasses = variablesToUtilities(
                    newClasses,
                    inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
                        lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap(),
                    inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
                        darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap()
                )
            }
            if (config.tailwind.prefix.isNotEmpty()) {
                newClasses = prefixClasses(newClasses, config.tailwind.prefix)
            }
            return newClasses
        }

        return Regex("className=(?:(?!>)[^\"'])*[\"']([^>]*)[\"']").replace(newContents) { result ->
            // matches any className, and takes everything inside the first quote to the last quote found before the closing `>`
            // if no quotes are found before the closing `>`, skips the match
            val match = result.groupValues[0]
            val group = result.groupValues[1]
            match.replace(
                group,
                // if the group contains a quote, we assume the classes are the last quoted string in the group
                if (group.contains("\"")) {
                    group.substringBeforeLast('"') + "\"" + handleClasses(
                        group.substringAfterLast('"')
                    )
                } else if (group.contains("'")) {
                    group.substringBeforeLast("'") + "'" + handleClasses(
                        group.substringAfterLast("'")
                    )
                } else handleClasses(group)
            )
        }
    }

    override fun fetchAllComponents(): List<ISPComponent> {
        val response = RequestSender.sendRequest("$domain/registry/index.json")
        return response.body?.let {
            Json.decodeFromString<List<ReactTypes.Component>>(it)
        }?.map { ISPComponent(it.name) } ?: emptyList()
    }

    override fun fetchAllStyles(): List<ISPStyle> {
        val response = RequestSender.sendRequest("$domain/registry/styles/index.json")
        return response.body?.let { Json.decodeFromString<List<ISPStyle>>(it) } ?: emptyList()
    }

    override fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            resolveAlias(getLocalConfig().aliases.components) + "/" + ReactTypes.ComponentKind.UI.name.lowercase()
        )?.children?.map { it.name }?.sorted() ?: emptyList()
    }

    override fun addComponent(componentName: String) {
        val installedComponents = getInstalledComponents()
        fun getRegistryDependencies(component: ReactTypes.ComponentWithContents): List<ReactTypes.ComponentWithContents> {
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
                val path = "${resolveAlias(config.aliases.components)}/${component.type.name.lowercase()}/${downloadedComponent.name}"
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    getConfigFileName(file.name),
                    FileTypeManager.getInstance().getFileTypeByExtension(getConfigFileName(file.name).substringAfterLast('.')),
                    adaptFileContents(file.content)
                )
                FileManager(project).saveFileAtPath(psiFile, path)
            }
        }

        // Install dependencies
        val manager = DependencyManager(project)
        val depsToInstall = component.dependencies.filter { dependency ->
            !manager.isDependencyInstalled(dependency)
        }
        if (depsToInstall.isEmpty()) return
        val dependenciesList = with(depsToInstall) {
            if (size == 1) first() else {
                "${dropLast(1).joinToString(", ")} and ${last()}"
            }
        }
        Notifications.Bus.notify(
            Notification(
                "shadcn/ui",
                "Installed ${component.name}",
                "${component.name} requires $dependenciesList to be installed.",
                NotificationType.INFORMATION
            ).apply {
                mapOf(
                    "Install" to DependencyManager.InstallationType.PROD,
                    "Install as dev" to DependencyManager.InstallationType.DEV
                ).forEach { (label, installType) ->
                    addAction(NotificationAction.createSimple(label) {
                        runAsync {
                            manager.installDependencies(depsToInstall, installType)
                        }.then {
                            Notifications.Bus.notifyAndHide(
                                Notification(
                                    "shadcn/ui",
                                    "Installed $dependenciesList",
                                    "Installed $dependenciesList for ${component.name}.",
                                    NotificationType.INFORMATION
                                ),
                                project
                            )
                        }
                        hideBalloon()
                    })
                }
            },
            project
        )
    }

    override fun isComponentUpToDate(componentName: String): Boolean {
        val config = getLocalConfig()
        val remoteComponent = fetchComponent(componentName)
        return remoteComponent.files.all { file ->
            FileManager(project).getFileContentsAtPath(
                "${resolveAlias(config.aliases.components)}/${remoteComponent.type.name.lowercase()}/${remoteComponent.name}/${getConfigFileName(file.name)}"
            ) == adaptFileContents(file.content)
        }
    }

    override fun removeComponent(componentName: String) {
        val remoteComponent = fetchComponent(componentName)
        FileManager(project).deleteFileAtPath(
            "${resolveAlias(getLocalConfig().aliases.components)}/${remoteComponent.type.name.lowercase()}/${remoteComponent.name}"
        )
    }
}

object ReactTypes {
    /**
     * The kind of component.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    enum class ComponentKind {
        @SerialName("components:ui")
        UI,
        @SerialName("components:component")
        COMPONENT,
        @SerialName("components:example")
        EXAMPLE
    }

    /**
     * A shadcn-svelte component in the registry.
     * @param name The name of the component.
     * @param dependencies The npm dependencies of the component.
     * @param registryDependencies The other components that this component depends on.
     * @param files The files that make up the component.
     * @param type The kind of component.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class Component(
        val name: String,
        val dependencies: List<String>,
        val registryDependencies: List<String>,
        val files: List<String>,
        val type: ComponentKind
    )

    /**
     * A shadcn-svelte component in the registry.
     * @param name The name of the component.
     * @param dependencies The npm dependencies of the component.
     * @param registryDependencies The other components that this component depends on.
     * @param files The files that make up the component.
     * @param type The kind of component.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class ComponentWithContents(
        val name: String,
        val dependencies: List<String>,
        val registryDependencies: List<String>,
        val files: List<File>,
        val type: ComponentKind
    ) {
        /**
         * A component's file.
         * @param name The name of the file.
         * @param content The contents of the file.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        data class File(
            val name: String,
            val content: String
        )
    }

    /**
     * A shadcn-svelte locally installed components.json file.
     * @param `$schema` The schema URL for the file.
     * @param style The library style used.
     * @param tailwind The Tailwind configuration.
     * @param rsc Whether to support React Server Components.
     * @param tsx Whether to use TypeScript over JavaScript.
     * @param aliases The aliases for the components and utils directories.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
    @Serializable
    data class Config(
        val `$schema`: String,
        val style: Styles,
        val tailwind: Tailwind,
        val rsc: Boolean,
        val tsx: Boolean = true,
        val aliases: Aliases
    ) {
        /**
         * The library style used.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        enum class Styles {
            @SerialName("default")
            DEFAULT,
            @SerialName("new-york")
            NEW_YORK
        }

        /**
         * The Tailwind configuration.
         * @param config The relative path to the Tailwind config file.
         * @param css The relative path of the Tailwind CSS file.
         * @param baseColor The library's base color.
         * @param cssVariables Whether to use CSS variables or utility classes.
         * @param prefix The prefix to use for utility classes.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        data class Tailwind(
            val config: String,
            val css: String,
            val baseColor: String,
            val cssVariables: Boolean,
            val prefix: String = ""
        )

        /**
         * The aliases for the components and utils directories.
         * @param components The alias for the components' directory.
         * @param utils The alias for the utils directory.
         * @param ui The alias for UI components.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        data class Aliases(
            val components: String,
            val utils: String,
            val ui: String? = null
        )
    }
}
