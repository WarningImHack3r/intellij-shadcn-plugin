package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 * @param `$schema` The schema URL for the file.
 * @param tailwind The Tailwind configuration.
 * @param uno The UnoCSS configuration.
 * @param aliases The aliases for the components and utils directories.
 */
@Suppress("kotlin:S117")
@Serializable
data class SolidConfig(
    override val `$schema`: String = "https://shadcn-solid.vercel.app/schema.json",
    override val tailwind: CssConfig? = null,
    val uno: CssConfig? = null,
    @SerialName("alias")
    override val aliases: Alias
) : Config() {

    /**
     * The Tailwind/UnoCSS configuration.
     * @param config The relative path to the Tailwind/Uno config file.
     * @param css The CSS settings to use.
     * @param color The library's base color.
     * @param prefix The prefix to use for utility classes.
     */
    @Serializable
    data class CssConfig(
        val config: String,
        val css: Css,
        val color: String,
        val prefix: String = ""
    ) : Tailwind() {

        /**
         * The CSS settings.
         * @param path The relative path of the Tailwind/Uno CSS file.
         * @param variable Whether to use CSS variables or utility classes.
         */
        @Serializable
        data class Css(
            val path: String,
            var variable: Boolean = true
        )
    }

    /**
     * The aliases for the components and utils directories.
     * @param ui The alias for the UI directory.
     */
    @Serializable
    data class Alias(
        @SerialName("component")
        override val components: String,
        @SerialName("cn")
        override val utils: String,
        val ui: String? = null
    ) : Aliases()
}
