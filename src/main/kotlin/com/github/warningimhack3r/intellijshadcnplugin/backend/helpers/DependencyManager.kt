package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Service(Service.Level.PROJECT)
class DependencyManager(private val project: Project) {
    companion object {
        private val log = logger<DependencyManager>()

        @JvmStatic
        fun getInstance(project: Project): DependencyManager = project.service()
    }

    enum class InstallationType {
        DEV,
        PROD
    }

    private fun getPackageManager(): String? {
        val fileManager = FileManager.getInstance(project)
        return mapOf(
            "package-lock.json" to "npm",
            "pnpm-lock.yaml" to "pnpm",
            "yarn.lock" to "yarn",
            "bun.lockb" to "bun"
        ).filter {
            fileManager.getVirtualFilesByName(it.key).isNotEmpty()
        }.values.firstOrNull()
    }

    private fun getInstallCommand(packageManager: String): String {
        return when (packageManager) {
            "npm" -> "i"
            "pnpm" -> "add"
            "yarn" -> "add"
            "bun" -> "add"
            else -> throw IllegalArgumentException("Unknown package manager: $packageManager")
        }
    }

    fun installDependencies(dependencyNames: List<String>, installationType: InstallationType = InstallationType.PROD) {
        getPackageManager()?.let { packageManager ->
            // install the dependency
            val command = listOfNotNull(
                packageManager,
                getInstallCommand(packageManager),
                if (installationType == InstallationType.DEV) "-D" else null,
                *dependencyNames.toTypedArray()
            ).toTypedArray()
            // check if the installation was successful
            if (ShellRunner.getInstance(project).execute(command) == null) {
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
            // check if the uninstallation was successful
            if (ShellRunner.getInstance(project).execute(command) == null) {
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
        return FileManager.getInstance(project).getFileContentsAtPath("package.json")?.let { packageJson ->
            Json.parseToJsonElement(packageJson).jsonObject.filter {
                it.key == "dependencies" || it.key == "devDependencies"
            }.map { it.value.jsonObject.keys }.flatten().also {
                log.debug("Installed dependencies: $it")
            }
        } ?: emptyList<String>().also {
            log.error("package.json not found")
        }
    }

    fun isDependencyInstalled(dependency: String): Boolean {
        // Read the package.json file
        return getInstalledDependencies().contains(dependency).also {
            logger<DependencyManager>().debug("Is $dependency installed? $it")
        }
    }
}
