package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull

object SvelteConfigDeserializer : JsonContentPolymorphicSerializer<SvelteConfig>(SvelteConfig::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out SvelteConfig> {
        val tsValue = element.asJsonObject?.get("typescript")
        return if (tsValue == null || tsValue.asJsonPrimitive?.booleanOrNull != null) SvelteConfigTsBoolean.serializer()
        else SvelteConfigTsObject.serializer()
    }
}

@Serializable
sealed class SvelteConfig : Config() {
    /**
     * The schema URL for the file.
     */
    override val `$schema`: String = "https://shadcn-svelte.com/schema.json"

    /**
     * The Tailwind configuration.
     */
    abstract override val tailwind: Tailwind

    /**
     * The aliases for the components and utils directories.
     */
    abstract override val aliases: Aliases

    /**
     * The Tailwind configuration.
     * @param css The relative path of the Tailwind CSS file.
     * @param baseColor The library's base color.
     */
    @Serializable
    data class Tailwind(
        val css: String,
        val baseColor: String
    ) : Config.Tailwind()

    /**
     * The aliases for the components and utils directories.
     */
    @Serializable
    data class Aliases(
        override val components: String,
        override val utils: String
    ) : Config.Aliases()
}

/**
 * A shadcn locally installed components.json file.
 *
 * @param typescript Whether to use TypeScript over JavaScript.
 */
@Serializable
data class SvelteConfigTsBoolean(
    override val tailwind: Tailwind,
    override val aliases: Aliases,
    val typescript: Boolean = true
) : SvelteConfig()

/**
 * A shadcn locally installed components.json file.
 *
 * @param typescript The TypeScript object.
 */
@Serializable
data class SvelteConfigTsObject(
    override val tailwind: Tailwind,
    override val aliases: Aliases,
    val typescript: TsObject
) : SvelteConfig() {

    /**
     * The inner `typescript` object.
     *
     * @param config The path to the `jsconfig`/`tsconfig`.
     */
    @Serializable
    data class TsObject(
        val config: String? = null
    )
}
