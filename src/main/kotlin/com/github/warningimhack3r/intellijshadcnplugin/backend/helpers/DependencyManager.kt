package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class DependencyManager(private val project: Project) {
    enum class InstallationType {
        DEV,
        PROD
    }

    private fun installDependency(dependencyName: String, installationType: InstallationType = InstallationType.PROD) {
        // get the package manager
        val fileManager = FileManager(project)
        val packageManager = mapOf(
            "package-lock.json" to "npm",
            "pnpm-lock.yaml" to "pnpm",
            "yarn.lock" to "yarn",
            "bun.lockb" to "bun"
        ).filter { runReadAction {
            fileManager.getVirtualFilesByName(it.key).isNotEmpty()
        } }.values.firstOrNull()
        // install the dependency
        ShellRunner(project).execute(listOfNotNull(
            packageManager,
            "i",
            if (installationType == InstallationType.DEV) "-D" else null,
            dependencyName
        ).toTypedArray())
    }

    fun installDependencies(dependencyNames: List<String>, installationType: InstallationType = InstallationType.PROD) {
        dependencyNames.forEach { installDependency(it, installationType) }
    }

    fun isDependencyInstalled(dependency: String): Boolean {
        // Read the package.json file
        return FileManager(project).getVirtualFilesByName("package.json").firstOrNull()?.let { packageJson ->
            val contents = packageJson.contentsToByteArray().decodeToString()
            Json.parseToJsonElement(contents).jsonObject.filter {
                it.key == "dependencies" || it.key == "devDependencies"
            }.map { it.value.jsonObject.keys }.flatten().contains(dependency)
            // Check if the dependency is installed
        } ?: false
    }
}
