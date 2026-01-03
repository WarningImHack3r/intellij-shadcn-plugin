package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement


object VisitorHelpers {
    /**
     * Attempts to load a static field from a class with fallbacks.
     * All the attempted fields should inherit from the same base class, which you can specify as the generic type parameter.
     *
     * It is strongly recommended to use this method with caution, as it uses reflection and can lead to runtime errors if the class or field does not exist.
     * It's advised that the passed arguments be given ordered from the least likely to the most likely to succeed.
     *
     * @param attempts A list of pairs containing the full class name and the field name to attempt to load.
     * @return The loaded field value, or null if all attempts fail.
     */
    @Suppress("kotlin:S6530", "UNCHECKED_CAST")
    fun <T> loadStaticFieldWithFallbacks(vararg attempts: Pair<String, String>): T? {
        for ((className, fieldName) in attempts) {
            try {
                val clazz = Class.forName(className)
                val field = clazz.getField(fieldName)
                return field[null] as T
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Attempts to load a static field from multiple different classes.
     *
     * This method is a convenience wrapper around the other {@link #loadStaticFieldWithFallbacks(vararg Pair<String, String>)} variant that allows you to pass a variable number of class names and a single field name to avoid repeatedly specifying an identical field name.
     *
     * @param fieldName The name of the static field to load.
     * @param classes Variable number of class names to attempt to load the field from.
     * @return The loaded field value, or null if all attempts fail.
     */
    fun <T> loadStaticFieldWithFallbacks(fieldName: String, vararg classes: String): T? {
        return loadStaticFieldWithFallbacks(*classes.associateWith { fieldName }.toList().toTypedArray())
    }
}
