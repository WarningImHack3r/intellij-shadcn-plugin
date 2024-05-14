package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class DependencyManager(private val project: Project) {
    enum class InstallationType {
        DEV,
        PROD
    }

    private fun getPackageManager(): String? {
        val fileManager = FileManager(project)
        return mapOf(
            "package-lock.json" to "npm",
            "pnpm-lock.yaml" to "pnpm",
            "yarn.lock" to "yarn",
            "bun.lockb" to "bun"
        ).filter {
            fileManager.getVirtualFilesByName(it.key).isNotEmpty()
        }.values.firstOrNull()
    }

    fun installDependencies(dependencyNames: List<String>, installationType: InstallationType = InstallationType.PROD) {
        getPackageManager()?.let { packageManager ->
            // install the dependency
            val command = listOfNotNull(
                packageManager,
                "i",
                if (installationType == InstallationType.DEV) "-D" else null,
                *dependencyNames.toTypedArray()
            ).toTypedArray()
            val res = ShellRunner(project).execute(command)
            // check if the installation was successful
            if (res == null) {
                NotificationManager(project).sendNotification(
                    "Failed to install dependencies",
                    "Failed to install dependencies: ${dependencyNames.joinToString { ", " }} (${command.joinToString(" ")}). Please install it manually.",
                    NotificationType.ERROR
                )
            }
        } ?: throw IllegalStateException("No package manager found")
    }

    fun uninstallDependencies(dependencyNames: List<String>) {
        getPackageManager()?.let { packageManager ->
            // uninstall the dependencies
            val command = listOf(
                packageManager,
                "remove",
                *dependencyNames.toTypedArray()
            ).toTypedArray()
            val res = ShellRunner(project).execute(command)
            // check if the uninstallation was successful
            if (res == null) {
                NotificationManager(project).sendNotification(
                    "Failed to uninstall dependencies",
                    "Failed to uninstall dependencies (${command.joinToString(" ")}). Please uninstall them manually.",
                    NotificationType.ERROR
                )
            }
        } ?: throw IllegalStateException("No package manager found")
    }

    fun getInstalledDependencies(): List<String> {
        // Read the package.json file
        return FileManager(project).getFileContentsAtPath("package.json")?.let { packageJson ->
            Json.parseToJsonElement(packageJson).jsonObject.filter {
                it.key == "dependencies" || it.key == "devDependencies"
            }.map { it.value.jsonObject.keys }.flatten().also {
                logger<DependencyManager>().debug("Installed dependencies: $it")
            }
        } ?: emptyList<String>().also {
            logger<DependencyManager>().error("package.json not found")
        }
    }

    fun isDependencyInstalled(dependency: String): Boolean {
        // Read the package.json file
        return getInstalledDependencies().contains(dependency).also {
            logger<DependencyManager>().debug("Is $dependency installed? $it")
        }
    }
}
