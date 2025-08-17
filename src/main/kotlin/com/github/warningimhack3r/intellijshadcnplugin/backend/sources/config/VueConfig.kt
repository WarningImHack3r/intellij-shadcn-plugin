package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param style The library style installed.
 * @param typescript Whether to use TypeScript over JavaScript.
 * @param tailwind The Tailwind configuration.
 * @param aliases The aliases for the components and utils directories.
 * @param iconLibrary The icon library
 */
@Suppress("kotlin:S117", "unused")
@Serializable
class VueConfig(
    override val `$schema`: String = "https://shadcn-vue.com/schema.json",
    val style: String,
    val typescript: Boolean = true,
    override val tailwind: Tailwind,
    override val aliases: Aliases,
    val iconLibrary: String? = null
) : Config() {

    /**
     * The Tailwind configuration.
     * @param config The relative path to the Tailwind config file.
     * @param css The relative path of the Tailwind CSS file.
     * @param baseColor The library's base color.
     * @param cssVariables Whether to use CSS variables instead of Tailwind utility classes.
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
     * @param composables The alias for the composables directory.
     * @param ui The alias for UI components.
     * @param lib The alias for the lib directory.
     */
    @Serializable
    data class Aliases(
        override val components: String,
        val composables: String? = null,
        override val utils: String,
        val ui: String? = null,
        val lib: String? = null
    ) : Config.Aliases()
}
