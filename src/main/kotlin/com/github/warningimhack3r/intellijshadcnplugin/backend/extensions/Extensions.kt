package com.github.warningimhack3r.intellijshadcnplugin.backend.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

fun <T> safeConversion(block: () -> T): T? = try {
    block()
} catch (_: IllegalArgumentException) {
    null
}

val JsonElement.asJsonObject
    get() = safeConversion { jsonObject }

val JsonElement.asJsonArray
    get() = safeConversion { jsonArray }
