package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class ISPSvelteSource(private val project: Project): ISPSource {
    override var domain = "https://www.shadcn-svelte.com"
    override var language = "Svelte"

    private fun fetchComponent(componentName: String): SvelteTypes.SvelteComponentWithContents {
        val style = getLocalConfig().style
        val response = RequestSender.sendRequest("$domain/registry/styles/$style/$componentName.json")
        return response.body?.let { Json.decodeFromString<SvelteTypes.SvelteComponentWithContents>(it) } ?: throw Exception("Component not found")
    }

    private fun getLocalConfig(): SvelteTypes.Config {
        return FileManager(project).getFileContentsAtPath("components.json")?.let {
            Json.decodeFromString(it)
        } ?: throw NoSuchFileException("components.json not found")
    }

    private fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias
        var tsConfig = FileManager(project).getFileContentsAtPath(".svelte-kit/tsconfig.json")
        if (tsConfig == null) {
            ShellRunner(project).execute(arrayOf("npx", "svelte-kit", "sync"))
            Thread.sleep(250) // wait for the sync to create the files
            tsConfig = FileManager(project).getFileContentsAtPath(".svelte-kit/tsconfig.json") ?: throw NoSuchFileException("Cannot get or generate .svelte-kit/tsconfig.json")
        }
        val aliasPath = Json.parseToJsonElement(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get(alias.substringBefore("/"))
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias")
        return aliasPath.substringAfter("/").plus("/").plus(alias.substringAfter("/"))
    }

    private fun replaceImports(contents: String): String {
        val config = getLocalConfig()
        return contents.replace(
            Regex("@/registry/[^/]+"), if (config.aliases.components.startsWith("\$")) {
                "\\${config.aliases.components}" // fixes Kotlin silently crashing when the replacement starts with $ with a regex
            } else config.aliases.components
        ).replace(
            "\$lib/utils", config.aliases.utils
        )
    }

    override fun fetchAllComponents(): List<ISPComponent> {
        val response = RequestSender.sendRequest("$domain/registry/index.json")
        return response.body?.let {
            Json.decodeFromString<List<SvelteTypes.SvelteComponent>>(it)
        }?.map { ISPComponent(it.name) } ?: emptyList()
    }

    override fun fetchAllStyles(): List<ISPStyle> {
        val response = RequestSender.sendRequest("$domain/registry/styles/index.json")
        return response.body?.let { Json.decodeFromString<List<ISPStyle>>(it) } ?: emptyList()
    }

    override fun getInstalledComponents(): List<String> {
        return FileManager(project).getFileAtPath(
            resolveAlias(getLocalConfig().aliases.components) + "/" + SvelteTypes.ComponentKind.UI.name.lowercase()
        )?.children?.map { it.name }?.sorted() ?: emptyList()
    }

    override fun addComponent(componentName: String) {
        fun getRegistryDependencies(component: SvelteTypes.SvelteComponentWithContents): List<SvelteTypes.SvelteComponentWithContents> {
            return component.registryDependencies.map { registryDependency ->
                val dependency = fetchComponent(registryDependency)
                listOf(dependency, *getRegistryDependencies(dependency).toTypedArray())
            }.flatten()
        }

        val component = fetchComponent(componentName)
        val components = listOf(component, *getRegistryDependencies(fetchComponent(componentName)).toTypedArray())
        val config = getLocalConfig()
        components.forEach { downloadedComponent ->
            downloadedComponent.files.forEach { file ->
                val path = "${resolveAlias(config.aliases.components)}/${component.type.name.lowercase()}/${downloadedComponent.name}"
                val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                    file.name,
                    FileTypeManager.getInstance().getFileTypeByExtension(file.name.substringAfterLast('.')),
                    replaceImports(file.content)
                )
                FileManager(project).saveFileAtPath(psiFile, path)
            }
        }
        // TODO: what to do with the dependencies to install? Notify or install them?
    }

    override fun isComponentUpToDate(componentName: String): Boolean {
        val config = getLocalConfig()
        val remoteComponent = fetchComponent(componentName)
        return remoteComponent.files.all { file ->
            FileManager(project).getFileContentsAtPath(
                "${resolveAlias(config.aliases.components)}/${remoteComponent.type.name.lowercase()}/${remoteComponent.name}/${file.name}"
            ) == replaceImports(file.content)
        }
    }

    override fun removeComponent(componentName: String) {
        val remoteComponent = fetchComponent(componentName)
        FileManager(project).deleteFileAtPath(
            "${resolveAlias(getLocalConfig().aliases.components)}/${remoteComponent.type.name.lowercase()}/${remoteComponent.name}"
        )
    }
}

object SvelteTypes {
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
     * @param type The kind of component (always "components:ui" for now).
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW") // https://github.com/Kotlin/kotlinx.serialization/issues/993#issuecomment-984742051
    @Serializable
    data class SvelteComponent(
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
     * @param type The kind of component (always "components:ui" for now).
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW")
    @Serializable
    data class SvelteComponentWithContents(
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
     * @param style The library style installed (currently "default" or "new-york").
     * @param tailwind The Tailwind configuration.
     * @param aliases The aliases for the components and utils directories.
     */
    @Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
    @Serializable
    data class Config(
        val `$schema`: String,
        val style: String,
        val tailwind: Tailwind,
        val aliases: Aliases
    ) {
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
            val baseColor: String
        )

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
