package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import kotlinx.serialization.Serializable

/**
 * A shadcn component in the registry in the list of components
 * like `index.json`.
 *
 * @param name The name of the component.
 * @param type The kind of component.
 * @param dependencies The npm dependencies of the component.
 * @param devDependencies The npm devDependencies of the component. Specific to shadcn-svelte.
 * @param registryDependencies The other components that this component depends on.
 */
@Serializable
data class Component(
    val name: String,
    val type: String,
    val dependencies: List<String> = emptyList(),
    val devDependencies: List<String> = emptyList(),
    val registryDependencies: List<String> = emptyList()
)
