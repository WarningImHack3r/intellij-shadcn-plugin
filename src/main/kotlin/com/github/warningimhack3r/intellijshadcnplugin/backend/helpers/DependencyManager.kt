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

    enum class PackageManager(val command: String) {
        NPM("npm"),
        PNPM("pnpm"),
        YARN("yarn"),
        BUN("bun");

        fun getLockFileName() = when (this) {
            NPM -> "package-lock.json"
            PNPM -> "pnpm-lock.yaml"
            YARN -> "yarn.lock"
            BUN -> "bun.lockb"
        }

        fun getInstallCommand() = when (this) {
            YARN -> "add"
            else -> "i"
        }
    }

    private fun getPackageManager(): PackageManager? {
        val fileManager = FileManager.getInstance(project)
        return PackageManager.entries.firstOrNull {
            fileManager.getVirtualFilesByName(it.getLockFileName()).isNotEmpty()
        }
    }

    fun installDependencies(dependencyNames: List<String>, installationType: InstallationType = InstallationType.PROD) {
        getPackageManager()?.let { packageManager ->
            // install the dependency
            val command = listOfNotNull(
                packageManager.command,
                packageManager.getInstallCommand(),
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
                packageManager.command,
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
