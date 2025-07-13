package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * A polymorphic (de)serializer used to differentiate `Component`s that have a list of string
 * from a one having a list of objects.
 *
 * @see <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#content-based-polymorphic-deserialization">Source</a>
 */
object ComponentDeserializer : JsonContentPolymorphicSerializer<Component>(Component::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Component> {
        val filesArray = element.jsonObject["files"]?.jsonArray ?: return StringFileComponent.serializer()
        return if (filesArray.isNotEmpty() && filesArray[0].asJsonPrimitive?.isString == true) {
            StringFileComponent.serializer()
        } else StructFileComponent.serializer()
    }
}

/**
 * A shadcn component in the registry in the list of components
 * like `index.json`.
 *
 * @param name The name of the component.
 * @param dependencies The npm dependencies of the component.
 * @param devDependencies The npm devDependencies of the component.
 * @param registryDependencies The other components that this component depends on.
 * @param type The kind of component.
 */
@Serializable
sealed class Component {
    abstract val name: String
    abstract val type: RegistryType
    abstract val dependencies: List<String>
    abstract val devDependencies: List<String>
    abstract val registryDependencies: List<String>

    /**
     * Enumerates all possible types of items in the shadcn registry.
     * Each type is annotated with its corresponding registry identifier.
     */
    @Suppress("unused")
    @Serializable
    enum class RegistryType {
        /** Represents a library */
        @SerialName("registry:lib")
        LIB,

        /** Represents a building block */
        @SerialName("registry:block")
        BLOCK,

        /** Represents a reusable component */
        @SerialName("registry:component")
        COMPONENT,

        /** Represents a UI element */
        @SerialName("registry:ui")
        UI,

        /** Represents a custom hook */
        @SerialName("registry:hook")
        HOOK,

        /** Represents a theme */
        @SerialName("registry:theme")
        THEME,

        /** Represents a complete page */
        @SerialName("registry:page")
        PAGE,

        /** Represents a simple file */
        @SerialName("registry:file")
        FILE,

        /** Represents a style */
        @SerialName("registry:style")
        STYLE,

        /** Represents a miscellaneous item */
        @SerialName("registry:item")
        ITEM,

        /** Represents an example (specific to shadcn-svelte) */
        @SerialName("registry:example")
        EXAMPLE,

        /** Represents an internal component (specific to shadcn-svelte) */
        @SerialName("registry:internal")
        INTERNAL
    }
}

/**
 * A component having paths as its `files` property.
 *
 * @param files The files that make up the component.
 */
@Serializable
data class StringFileComponent(
    override val name: String,
    override val type: RegistryType,
    override val dependencies: List<String> = emptyList(),
    override val devDependencies: List<String> = emptyList(),
    override val registryDependencies: List<String> = emptyList(),
    val files: List<String> = emptyList()
) : Component()

/**
 * A component having objects as its `files` property.
 *
 * @param files The files that make up the component.
 */
@Serializable
data class StructFileComponent(
    override val name: String,
    override val type: RegistryType,
    override val dependencies: List<String> = emptyList(),
    override val devDependencies: List<String> = emptyList(),
    override val registryDependencies: List<String> = emptyList(),
    val files: List<File> = emptyList()
) : Component() {
    /**
     * Represents a file belonging to a component.
     *
     * @param path The path of the file
     * @param type The kind of component
     */
    @Serializable
    data class File(
        val path: String,
        val type: RegistryType
    )
}
