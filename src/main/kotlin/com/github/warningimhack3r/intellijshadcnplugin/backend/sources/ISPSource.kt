package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

interface ISPSource {

    var domain: String
    var language: String

    fun fetchAllComponents(): List<ISPComponent>

    fun fetchAllStyles(): List<ISPStyle>

    fun getInstalledComponents(): List<String>

    fun addComponent(componentName: String)

    fun isComponentUpToDate(componentName: String): Boolean

    fun removeComponent(componentName: String)
}
