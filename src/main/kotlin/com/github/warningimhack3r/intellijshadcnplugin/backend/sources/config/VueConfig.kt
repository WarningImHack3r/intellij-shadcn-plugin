package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A shadcn-svelte locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param style The library style installed.
 * @param typescript Whether to use TypeScript over JavaScript.
 * @param tailwind The Tailwind configuration.
 * @param framework The Vue framework to use.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class VueConfig(
    override val `$schema`: String = "https://shadcn-vue.com/schema.json",
    override val style: String,
    val typescript: Boolean = true,
    override val tailwind: Tailwind,
    val framework: Framework = Framework.VITE,
    override val aliases: Aliases
) : Config() {

    /**
     * The Tailwind configuration.
     * @param config The relative path to the Tailwind config file.
     * @param css The relative path of the Tailwind CSS file.
     * @param baseColor The library's base color.
     * @param cssVariables Whether to use CSS variables instead of Tailwind utility classes.
     */
    @Serializable
    open class Tailwind(
        override val config: String,
        override val css: String,
        override val baseColor: String,
        open val cssVariables: Boolean = true
    ) : Config.Tailwind()

    /**
     * The framework used.
     */
    @Suppress("unused")
    @Serializable
    enum class Framework {
        @SerialName("vite")
        VITE,
        @SerialName("nuxt")
        NUXT,
        @SerialName("laravel")
        LARAVEL,
        @SerialName("astro")
        ASTRO
    }

    @Serializable
    class Aliases(
        override val components: String,
        override val utils: String,
    ) : Config.Aliases()
}
