package com.github.warningimhack3r.intellijshadcnplugin.backend.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun <T> safeConversion(block: () -> T): T? = runCatching(block).getOrNull()

val JsonElement.asJsonObject
    get() = safeConversion { jsonObject }

val JsonElement.asJsonArray
    get() = safeConversion { jsonArray }

val JsonElement.asJsonPrimitive
    get() = safeConversion { jsonPrimitive }
