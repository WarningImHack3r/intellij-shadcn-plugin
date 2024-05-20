package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn-svelte locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param tailwind The Tailwind configuration.
 * @param uno The UnoCSS configuration.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class SolidConfig(
    override val `$schema`: String = "",
    override val tailwind: Tailwind? = null,
    val uno: Uno? = null,
    override val aliases: Aliases
) : Config() {

    /**
     * The Tailwind configuration.
     * @param config The relative path to the Tailwind config file.
     * @param css The relative path of the Tailwind CSS file.
     * @param baseColor The library's base color.
     * @param cssVariables Whether to use CSS variables or utility classes.
     * @param prefix The prefix to use for utility classes.
     */
    @Serializable
    class Tailwind(
        override val config: String,
        override val css: String,
        val baseColor: String,
        val cssVariables: Boolean = true,
        val prefix: String = ""
    ) : Config.Tailwind()

    /**
     * The UnoCSS configuration.
     * @param config The relative path to the UnoCSS config file.
     * @param css The relative path of the UnoCSS file.
     * @param baseColor The library's base color.
     * @param cssVariables Whether to use CSS variables or utility classes.
     * @param prefix The prefix to use for utility classes.
     */
    @Serializable
    class Uno(
        override val config: String,
        override val css: String,
        val baseColor: String,
        val cssVariables: Boolean = true,
        val prefix: String = ""
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     * @param components The alias for the components' directory.
     * @param utils The alias for the utils directory.
     * @param ui The alias for the UI directory.
     */
    @Serializable
    class Aliases(
        val components: String,
        val utils: String,
        val ui: String? = null
    ) : Config.Aliases()
}
