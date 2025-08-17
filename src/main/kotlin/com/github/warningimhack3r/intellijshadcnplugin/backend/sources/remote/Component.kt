package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import kotlinx.serialization.Serializable

/**
 * A shadcn component in the registry in the list of components
 * like `index.json`.
 */
@Serializable
data class Component(
    /**
     * The name of the component.
     */
    val name: String,

    /**
     * The kind of component.
     */
    val type: String,

    /**
     * The npm dependencies of the component.
     */
    val dependencies: List<String>,

    /**
     * The npm devDependencies of the component. Specific to shadcn-svelte.
     */
    val devDependencies: List<String>,

    /**
     * The other components that this component depends on.
     */
    val registryDependencies: List<String>
)
