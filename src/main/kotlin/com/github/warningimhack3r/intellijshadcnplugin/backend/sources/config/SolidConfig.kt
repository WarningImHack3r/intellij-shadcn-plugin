package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW", "kotlin:S117")
@Serializable
class SolidConfig(
    override val `$schema`: String,
    override val style: String,
    override val tailwind: VueConfig.Tailwind,
    override val aliases: Aliases
) : Config() {

    @Serializable
    class Aliases(
        override val components: String,
        override val utils: String
    ) : Config.Aliases()
}
