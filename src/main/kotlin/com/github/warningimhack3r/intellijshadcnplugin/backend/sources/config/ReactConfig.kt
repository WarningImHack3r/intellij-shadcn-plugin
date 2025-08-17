package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param style The library's style used.
 * @param rsc Whether to support React Server Components.
 * @param tsx Whether to use TypeScript over JavaScript.
 * @param tailwind The Tailwind configuration.
 * @param aliases The aliases for the components and utils directories.
 * @param iconLibrary The icon library
 */
@Suppress("kotlin:S117")
@Serializable
data class ReactConfig(
    override val `$schema`: String = "https://ui.shadcn.com/schema.json",
    val style: String,
    val rsc: Boolean = false,
    val tsx: Boolean = true,
    override val tailwind: Tailwind,
    override val aliases: Aliases,
    val iconLibrary: String? = null
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
    data class Tailwind(
        val config: String? = null,
        val css: String,
        val baseColor: String,
        val cssVariables: Boolean = true,
        val prefix: String = ""
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     * @param ui The alias for UI components.
     * @param lib The alias for the library components.
     * @param hooks The alias for the hooks directory.
     */
    @Serializable
    data class Aliases(
        override val components: String,
        override val utils: String,
        val ui: String? = null,
        val lib: String? = null,
        val hooks: String? = null
    ) : Config.Aliases()
}
