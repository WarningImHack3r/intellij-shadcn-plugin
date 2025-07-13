package com.github.warningimhack3r.intellijshadcnplugin.backend.extensions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

val JsonElement.asJsonPrimitive
    get() = try {
        jsonPrimitive
    } catch (_: IllegalArgumentException) {
        null
    }
