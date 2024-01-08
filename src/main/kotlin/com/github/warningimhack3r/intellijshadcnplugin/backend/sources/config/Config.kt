package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn-svelte locally installed components.json file.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
sealed class Config {
    /**
     * The schema URL for the file.
     */
    abstract val `$schema`: String
    /**
     * The library's style used.
     */
    abstract val style: String
    /**
     * The Tailwind configuration.
     */
    abstract val tailwind: Tailwind
    /**
     * The aliases for the components and utils directories.
     */
    abstract val aliases: Aliases

    /**
     * The Tailwind configuration.
     */
    @Serializable
    sealed class Tailwind {
        /**
         * The relative path to the Tailwind config file.
         */
        abstract val config: String
        /**
         * The relative path of the Tailwind CSS file.
         */
        abstract val css: String
        /**
         * The library's base color.
         */
        abstract val baseColor: String
    }

    /**
     * The aliases for the components and utils directories.
     */
    @Serializable
    sealed class Aliases {
        /**
         * The alias for the components' directory.
         */
        abstract val components: String
        /**
         * The alias for the utils directory.
         */
        abstract val utils: String
    }
}