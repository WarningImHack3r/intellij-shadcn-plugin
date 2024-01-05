package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.DependencyManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
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

class ISPVueSource(private val project: Project): ISPSource {
    override var domain = "https://www.shadcn-vue.com"
    override var language = "Vue"

    private fun fetchComponent(componentName: String): VueTypes.ComponentWithContents {
        val style = getLocalConfig().style
        val response = RequestSender.sendRequest("$domain/registry/styles/$style/$componentName.json")
        return response.body?.let { Json.decodeFromString<VueTypes.ComponentWithContents>(it) } ?: throw Exception("Component not found")
    }

    private fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        val response = RequestSender.sendRequest("$domain/registry/colors/$baseColor.json")
        return response.body?.let { Json.parseToJsonElement(it) } ?: throw Exception("Colors not found")
    }

    private fun getLocalConfig(): VueTypes.Config {
        return FileManager(project).getFileContentsAtPath("components.json")?.let {
            Json.decodeFromString(it)
        } ?: throw NoSuchFileException("components.json not found")
    }

    private fun getConfigFileName(fileName: String): String {
        return if (!getLocalConfig().typescript) {
            fileName.replace(
                Regex("\\.ts$"),
                ".js"
            )
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
        val newContents = contents.replace(
            Regex("@/lib/registry/[^/]+"), cleanAlias(config.aliases.components)
        ).replace(
            // Note: this does not prevent additional imports other than "cn" from being replaced,
            // but I'm once again following what the original code does for parity
            // (https://github.com/radix-vue/shadcn-vue/blob/9d9a6f929ce0f281b4af36161af80ed2bbdc4a16/packages/cli/src/utils/transformers/transform-import.ts#L19-L29).
            Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"@/lib/utils"),
            cleanAlias(config.aliases.utils)
        ).applyIf(!config.typescript) {
            // TODO: detype Vue file
            this
        }
        return if (!config.tailwind.cssVariables) {
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
                val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject ?: throw Exception("Inline colors not found")
                return variablesToUtilities(
                    classes,
                    inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
                        lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap(),
                    inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
                        darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap()
                )
            }

            val notTemplateClasses = Regex("[^:]class=(?:(?!>)[^\"'])*[\"']([^\"']*)[\"']").replace(newContents) { result ->
                result.groupValues[0].replace(
                    result.groupValues[1],
                    handleClasses(result.groupValues[1])
                )
            }
            // Double quoted templates
            Regex(":class=(?:(?!>)[^\"])*\"([^\"]*)\"").replace(notTemplateClasses) { result ->
                val group = result.groupValues[1]
                result.groupValues[0].replace(
                    group,
                    handleClasses(group
                        .replace("\n", " ")
                        .split(", ")
                        .map { it.trim() }
                        .last { it.startsWith("'") || it.endsWith("'") })
                )
            }
        } else newContents
    }

    override fun fetchAllComponents(): List<ISPComponent> {
        val response = RequestSender.sendRequest("$domain/registry/index.json")
        return response.body?.let {
            Json.decodeFromString<List<VueTypes.Component>>(it)
        }?.map { ISPComponent(it.name) } ?: emptyList()
    }

    override fun fetchAllStyles(): List<ISPStyle> {
        val response = RequestSender.sendRequest("$domain/registry/styles/index.json")
        return response.body?.let { Json.decodeFromString<List<ISPStyle>>(it) } ?: emptyList()
    }

    override fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            resolveAlias(getLocalConfig().aliases.components) + "/" + VueTypes.ComponentKind.UI.name.lowercase()
        )?.children?.map { it.name }?.sorted() ?: emptyList()
    }

    override fun addComponent(componentName: String) {
        val installedComponents = getInstalledComponents()
        fun getRegistryDependencies(component: VueTypes.ComponentWithContents): List<VueTypes.ComponentWithContents> {
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

object VueTypes {
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
    @Suppress("PROVIDED_RUNTIME_TOO_LOW") // https://github.com/Kotlin/kotlinx.serialization/issues/993#issuecomment-984742051
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
     * @param style The library style installed.
     * @param tailwind The Tailwind configuration.
     * @param aliases The aliases for the components and utils directories.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
    @Serializable
    data class Config(
        val `$schema`: String,
        val style: Styles,
        val typescript: Boolean = true,
        val tailwind: Tailwind,
        val framework: Framework = Framework.VITE,
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
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        data class Tailwind(
            val config: String,
            val css: String,
            val baseColor: String,
            val cssVariables: Boolean = true
        )

        /**
         * The framework used.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        enum class Framework {
            @SerialName("vite")
            VITE,
            @SerialName("nuxt")
            NUXT,
            @SerialName("laravel")
            LARAVEL,
            @SerialName("astro")
            ASTRO
        }

        /**
         * The aliases for the components and utils directories.
         * @param components The alias for the components directory.
         * @param utils The alias for the utils directory.
         */
        @Suppress("PROVIDED_RUNTIME_TOO_LOW")
        @Serializable
        data class Aliases(
            val components: String,
            val utils: String
        )
    }
}
