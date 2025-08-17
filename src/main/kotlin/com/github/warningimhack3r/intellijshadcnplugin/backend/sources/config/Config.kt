package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 */
@Suppress("unused", "PropertyName")
@Serializable
sealed class Config {
    /**
     * The schema URL for the file.
     */
    abstract val `$schema`: String

    /**
     * The Tailwind configuration.
     */
    abstract val tailwind: Tailwind?

    /**
     * The aliases for the components and utils directories.
     */
    abstract val aliases: Aliases

    /**
     * The Tailwind configuration.
     */
    @Serializable
    sealed class Tailwind

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
