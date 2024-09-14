package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

/**
 * A shadcn locally installed components.json file.
 */
@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117", "unused", "PropertyName")
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
    sealed class Tailwind {
        /**
         * The relative path to the Tailwind config file.
         */
        abstract val config: String

        /**
         * The relative path of the Tailwind CSS file.
         */
        abstract val css: String
    }

    /**
     * The aliases for the components and utils directories.
     */
    @Serializable
    sealed class Aliases
}
