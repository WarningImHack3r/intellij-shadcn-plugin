package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A v0 component in its raw form.
 * @param name The name of the component.
 * @param type The kind of component.
 * @param dependencies The npm dependencies of the component.
 * @param devDependencies The npm devDependencies of the component.
 * @param registryDependencies The other components that this component depends on.
 * @param files The files that make up the component.
 * @param tailwind The Tailwind configuration for the component.
 * @param cssVars The CSS variables for the component.
 * @param meta The import and module specifiers for the component.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class V0Component(
    val name: String,
    val type: Type,
    val description: String = "",
    val dependencies: List<String> = emptyList(),
    val devDependencies: List<String> = emptyList(),
    val registryDependencies: List<String> = emptyList(),
    val files: List<V0File> = emptyList(),
    val tailwind: Tailwind = Tailwind(),
    val cssVars: CssVars = CssVars(),
    val meta: Map<String, String> = emptyMap()
) {
    /**
     * The kind of component.
     */
    @Suppress("unused")
    @Serializable
    enum class Type {
        @SerialName("registry:style")
        STYLE,

        @SerialName("registry:lib")
        LIB,

        @SerialName("registry:example")
        EXAMPLE,

        @SerialName("registry:block")
        BLOCK,

        @SerialName("registry:component")
        COMPONENT,

        @SerialName("registry:ui")
        UI,

        @SerialName("registry:hook")
        HOOK,

        @SerialName("registry:theme")
        THEME,

        @SerialName("registry:page")
        PAGE
    }


    /**
     * A component's file.
     * @param path The path and name of the file, relative to the lib directory.
     * @param type The kind of component.
     * @param content The content of the file.
     */
    @Serializable
    data class V0File(
        val path: String,
        val type: Type,
        val content: String
    )

    /**
     * Tailwind configuration for a component.
     * @param config The Tailwind configuration for the component.
     */
    @Serializable
    data class Tailwind(
        val config: TailwindConfig = TailwindConfig()
    ) {
        /**
         * Tailwind configuration for a component.
         * @param content
         * @param theme
         * @param plugins
         */
        @Serializable
        data class TailwindConfig(
            val content: List<String> = emptyList(),
            val theme: Map<String, String> = emptyMap(),
            val plugins: List<String> = emptyList()
        )
    }

    /**
     * CSS variables for a component.
     * @param light The light mode CSS variables.
     * @param dark The dark mode CSS variables.
     */
    @Serializable
    data class CssVars(
        val light: Map<String, String> = emptyMap(),
        val dark: Map<String, String> = emptyMap()
    )
}
