package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

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
        val fileManager = FileManager.getInstance(project)
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
            *dependenciesNames.let { deps ->
                if (packageManager in listOf(PackageManager.DENO)) {
                    deps.map { "npm:$it" }
                } else deps
            }.toTypedArray()
        ).toTypedArray()
        // check if the installation was successful
        (ShellRunner.getInstance(project).execute(command) != null).also { success ->
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
        (ShellRunner.getInstance(project).execute(command) != null).also { success ->
            if (!success) {
                log.warn("Failed to uninstall dependencies (${command.joinToString(" ")}).")
            }
        }
    } ?: throw IllegalStateException("No package manager found")

    fun getInstalledDependencies() =
        FileManager.getInstance(project).getFileContentsAtPath("package.json")?.let { packageJson ->
            Json.parseToJsonElement(packageJson).jsonObject.filter {
                it.key == "dependencies" || it.key == "devDependencies"
            }.map { it.value.jsonObject.keys }.flatten().also {
                log.debug("Installed dependencies: $it")
            }
        } ?: emptyList<String>().also { log.error("package.json not found") }

    fun isDependencyInstalled(dependency: String) = getInstalledDependencies().contains(dependency).also {
        logger<DependencyManager>().debug("Is $dependency installed? $it")
    }
}
