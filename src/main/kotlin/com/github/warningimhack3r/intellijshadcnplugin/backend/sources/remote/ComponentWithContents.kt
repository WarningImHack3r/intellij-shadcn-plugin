package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import kotlinx.serialization.Serializable

/**
 * A shadcn component in the registry.
 *
 * @param name The name of the component.
 * @param dependencies The npm dependencies of the component.
 * @param registryDependencies The other components that this component depends on.
 * @param files The files that make up the component.
 * @param type The kind of component.
 */
@Serializable
data class ComponentWithContents(
    val name: String,
    val dependencies: List<String> = emptyList(),
    val registryDependencies: List<String> = emptyList(),
    val files: List<File>,
    val type: String
) {
    /**
     * A component's file.
     * @param name The name of the file.
     * @param content The contents of the file.
     */
    @Serializable
    data class File(
        val name: String,
        val content: String
    )
}
