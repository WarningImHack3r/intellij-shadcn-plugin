package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote

import kotlinx.serialization.Serializable

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
    val type: String
)
