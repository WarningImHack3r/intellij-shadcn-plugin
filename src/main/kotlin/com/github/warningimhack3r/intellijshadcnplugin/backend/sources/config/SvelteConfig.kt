package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class SvelteConfig(
    override val `$schema`: String,
    override val style: String,
    override val tailwind: Tailwind,
    override val aliases: Aliases
) : Config() {

    @Serializable
    class Tailwind(
        override val config: String,
        override val css: String,
        override val baseColor: String
    ) : Config.Tailwind()

    @Serializable
    class Aliases(
        override val components: String,
        override val utils: String
    ) : Config.Aliases()
}
