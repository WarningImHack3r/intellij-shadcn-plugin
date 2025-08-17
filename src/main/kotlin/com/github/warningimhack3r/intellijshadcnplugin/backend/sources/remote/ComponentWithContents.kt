package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonArray
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

object ComponentWithContentsDeserializer : JsonContentPolymorphicSerializer<ComponentWithContents>(
    ComponentWithContents::class
) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out ComponentWithContents> {
        val filesArray = element.asJsonObject?.get("files")?.asJsonArray
            ?: return ComponentWithContentsLegacyFiles.serializer()
        return if (filesArray.isNotEmpty() && filesArray.first().asJsonObject?.keys?.contains("name") == true) {
            ComponentWithContentsLegacyFiles.serializer()
        } else ComponentWithContentsNewFiles.serializer()
    }
}

@Serializable
sealed class ComponentWithContents {
    /**
     * The name of the component.
     */
    abstract val name: String

    /**
     * The kind of component.
     */
    abstract val type: String

    /**
     * The npm dependencies of the component.
     */
    abstract val dependencies: List<String>

    /**
     * The npm devDependencies of the component.
     */
    abstract val devDependencies: List<String>

    /**
     * The other components that this component depends on.
     */
    abstract val registryDependencies: List<String>

    /**
     * A convenience wrapper to get all files paths independently of
     * the subclass.
     */
    val filePaths
        get() = when (this) {
            is ComponentWithContentsNewFiles -> files.map { it.filePath.substringAfterLast("/") }
            is ComponentWithContentsLegacyFiles -> files.map { it.name }
        }

    /**
     * A convenience wrapper to get all files paths independently of
     * the subclass.
     */
    val fileNames
        get() = filePaths.map { it.substringAfterLast("/") }
}

/**
 * An individual shadcn component in the registry.
 *
 * @param files The files of the component
 */
@Suppress("kotlin:S117", "PropertyName")
@Serializable
data class ComponentWithContentsNewFiles(
    val `$schema`: String = "",
    override val name: String,
    override val type: String,
    override val dependencies: List<String> = emptyList(),
    override val devDependencies: List<String> = emptyList(),
    override val registryDependencies: List<String> = emptyList(),
    val files: List<File> = emptyList()
) : ComponentWithContents() {
    /**
     * A component's file.
     *
     * @param path The path of the file relative to the registry root.
     * @param content The contents of the file.
     * @param type The kind of component.
     * @param target The path of the file relative to the project.
     */
    @Serializable
    data class File(
        val path: String = "", // `ui/...` (shadcn/shadcn-vue)
        val content: String = "",
        val type: String, // `registry:ui` (shadcn/shadcn-vue) / `registry:file` (shadcn-svelte)
        val target: String = "" // `accordion/accordion-content.svelte` (shadcn-svelte)
    ) {
        val filePath: String
            get() = target.ifEmpty { path }
    }
}

/**
 * A shadcn component in the registry, with the "old"
 * format of the `files` object.
 *
 * @param files The files of the component
 */
@Suppress("kotlin:S117", "PropertyName")
@Serializable
data class ComponentWithContentsLegacyFiles(
    val `$schema`: String = "",
    override val name: String,
    override val type: String,
    override val dependencies: List<String> = emptyList(),
    override val devDependencies: List<String> = emptyList(),
    override val registryDependencies: List<String> = emptyList(),
    val files: List<File> = emptyList()
) : ComponentWithContents() {
    /**
     * A component's file.
     *
     * @param name The file name.
     * @param content The contents of the file.
     */
    @Serializable
    data class File(
        val name: String, // `accordion.tsx` with `components:ui` type above (shadcn-solid) or `ui` (solid-ui)
        val content: String
    )
}
