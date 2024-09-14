package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param tsx Whether to use TypeScript over JavaScript.
 * @param componentDir The components' directory.
 * @param tailwind The Tailwind configuration.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class SolidUIConfig(
    override val `$schema`: String = "",
    val tsx: Boolean,
    val componentDir: String,
    override val tailwind: Tailwind,
    override val aliases: Aliases
) : Config() {

    /**
     * The Tailwind configuration.
     * @param config The relative path to the Tailwind config file.
     * @param css The relative path of the Tailwind CSS file.
     */
    @Serializable
    class Tailwind(
        override val config: String,
        override val css: String
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     * @param path The import alias for the `src` directory.
     */
    @Serializable
    class Aliases(
        val path: String,
    ) : Config.Aliases()
}
