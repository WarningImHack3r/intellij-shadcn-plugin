package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File

class ShellRunner(private val project: Project? = null) {
    companion object {
        private val log = logger<ShellRunner>()
    }
    private val failedCommands = mutableSetOf<String>()

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

    fun execute(command: Array<String>): String? {
        val commandName = command.firstOrNull() ?: return null.also {
            log.warn("No command name provided")
        }
        if (isWindows() && failedCommands.contains(commandName)) {
            command[0] = "$commandName.cmd"
        }
        return try {
            val platformCommand = if (isWindows()) {
                arrayOf("cmd", "/c")
            } else {
                emptyArray()
            } + command
            log.debug("Executing command: \"${platformCommand.joinToString(" ")}\"")
            val process = ProcessBuilder(*platformCommand)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .directory(project?.basePath?.let { File(it) })
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().readText().also {
                log.debug("Successfully executed \"${platformCommand.joinToString(" ")}\": $it")
            }
        } catch (e: Exception) {
            if (isWindows() && !commandName.endsWith(".cmd")) {
                log.warn("Failed to execute \"${command.joinToString(" ")}\". Trying to execute \"$commandName.cmd\" instead", e)
                failedCommands.add(commandName)
                return execute(arrayOf("$commandName.cmd") + command.drop(1).toTypedArray())
            }
            log.warn("Error while executing \"${command.joinToString(" ")}\"", e)
            null
        }
    }
}
