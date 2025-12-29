package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class DependencyManager(private val project: Project) {
    companion object {
        private val log = logger<DependencyManager>()

        /**
         * Remove the hardcoded version from a dependency
         */
        fun cleanDependency(dependency: String) = dependency.replace(Regex("@\\W+?[\\w.-]+$"), "")
    }

    enum class InstallationType {
        DEV,
        PROD
    }

    enum class PackageManager(val command: String) {
        NPM("npm"),
        DENO("deno"),
        PNPM("pnpm"),
        YARN("yarn"),
        BUN("bun");

        fun getLockFilesNames() = when (this) {
            NPM -> listOf("package-lock.json")
            DENO -> listOf("deno.lock")
            PNPM -> listOf("pnpm-lock.yaml")
            YARN -> listOf("yarn.lock")
            BUN -> listOf("bun.lockb", "bun.lock")
        }

        fun getInstallCommand() = when (this) {
            YARN -> "add"
            else -> "i"
        }
    }

    private fun getPackageManager(): PackageManager? {
        val fileManager = project.service<FileManager>()
        return PackageManager.entries.firstOrNull { packageManager ->
            packageManager.getLockFilesNames().any { lockFile ->
                fileManager.getVirtualFilesByName(lockFile).isNotEmpty()
            }
        }
    }

    fun installDependencies(
        dependenciesNames: List<String>,
        installationType: InstallationType = InstallationType.PROD
    ) = getPackageManager()?.let { packageManager ->
        // install the dependencies
        val command = listOfNotNull(
            packageManager.command,
            packageManager.getInstallCommand(),
            if (installationType == InstallationType.DEV) "-D" else null,
            *dependenciesNames.let { dependencies ->
                if (packageManager in listOf(PackageManager.DENO)) {
                    dependencies.map { "npm:$it" }
                } else dependencies
            }.toTypedArray()
        ).toTypedArray()
        // check if the installation was successful
        (project.service<ShellRunner>().execute(command) != null).also { success ->
            if (!success) {
                log.warn("Failed to install dependencies (${command.joinToString(" ")}).")
            }
        }
    } ?: throw IllegalStateException("No package manager found")

    fun uninstallDependencies(dependenciesNames: List<String>) = getPackageManager()?.let { packageManager ->
        // uninstall the dependencies
        val command = listOf(
            packageManager.command,
            "remove",
            *dependenciesNames.toTypedArray()
        ).toTypedArray()
        // check if the uninstallation was successful
        (project.service<ShellRunner>().execute(command) != null).also { success ->
            if (!success) {
                log.warn("Failed to uninstall dependencies (${command.joinToString(" ")}).")
            }
        }
    } ?: throw IllegalStateException("No package manager found")

    fun getInstalledDependencies() =
        project.service<FileManager>().getFileContentsAtPath("package.json")?.let { packageJson ->
            Json.parseToJsonElement(packageJson).asJsonObject?.filterKeys {
                it == "dependencies" || it == "devDependencies"
            }?.values?.mapNotNull { it.asJsonObject?.keys }?.flatten()?.distinct()?.also {
                log.debug("Installed dependencies: $it")
            }
        } ?: emptyList<String>().also { log.error("package.json not found") }

    fun isDependencyInstalled(dependency: String) = getInstalledDependencies().contains(dependency).also {
        logger<DependencyManager>().debug("Is $dependency installed? $it")
    }
}
