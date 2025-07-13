package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param tailwind The Tailwind configuration.
 * @param aliases The aliases for the components and utils directories.
 * @param typescript Whether to use TypeScript over JavaScript.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class SvelteConfig(
    override val `$schema`: String = "https://shadcn-svelte.com/schema.json",
    override val tailwind: Tailwind,
    override val aliases: Aliases,
    val typescript: Boolean = true
) : Config() {

    /**
     * The Tailwind configuration.
     * @param css The relative path of the Tailwind CSS file.
     * @param baseColor The library's base color.
     */
    @Serializable
    class Tailwind(
        override val css: String,
        val baseColor: String
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     * @param components The alias for the components' directory.
     * @param utils The alias for the utils directory.
     * @param ui The alias for the ui components directory.
     * @param hooks The alias for the hooks' directory.
     * @param lib The alias for the lib directory.
     */
    @Serializable
    class Aliases(
        val components: String,
        val utils: String,
        val ui: String,
        val hooks: String,
        val lib: String
    ) : Config.Aliases()
}
