package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param tsx Whether to use TypeScript over JavaScript.
 * @param tailwind The Tailwind configuration.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("kotlin:S117")
@Serializable
data class SolidUIConfig(
    override val `$schema`: String = "https://www.solid-ui.com/schema.json",
    val tsx: Boolean,
    override val tailwind: Tailwind,
    override val aliases: Aliases
) : Config() {

    /**
     * The Tailwind configuration.
     * @param css The relative path of the Tailwind CSS file.
     * @param config The relative path to the Tailwind config file.
     * @param prefix The prefix to use for utility classes.
     */
    @Serializable
    data class Tailwind(
        val css: String,
        val config: String? = null,
        val prefix: String = ""
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     */
    @Serializable
    data class Aliases(
        override val components: String,
        override val utils: String
    ) : Config.Aliases()
}
