package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn-svelte locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param style The library style used.
 * @param tailwind The Tailwind configuration.
 * @param rsc Whether to support React Server Components.
 * @param tsx Whether to use TypeScript over JavaScript.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class ReactConfig(
    override val `$schema`: String = "https://ui.shadcn.com/schema.json",
    override val style: String,
    val rsc: Boolean = false,
    val tsx: Boolean = true,
    override val tailwind: Tailwind,
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
        override val baseColor: String,
        val cssVariables: Boolean = true,
        val prefix: String = ""
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     * @param components The alias for the components' directory.
     * @param utils The alias for the utils directory.
     * @param ui The alias for UI components.
     */
    @Serializable
    class Aliases(
        override val components: String,
        override val utils: String,
        val ui: String? = null
    ) : Config.Aliases()
}
