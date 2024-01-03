package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.printlnError
import java.io.File

class ShellRunner(private val project: Project? = null) {
    private val failedCommands = mutableSetOf<String>()

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")

    fun execute(command: Array<String>): String? {
        val commandName = command.firstOrNull() ?: return null
        if (isWindows() && failedCommands.contains(commandName)) {
            command[0] = "$commandName.cmd"
        }
        return try {
            val process = ProcessBuilder(*command)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .directory(project?.basePath?.let { File(it) })
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            if (isWindows() && !commandName.endsWith(".cmd")) {
                failedCommands.add(commandName)
                return execute(arrayOf("$commandName.cmd") + command.drop(1).toTypedArray())
            }
            printlnError("Error while executing \"${command.joinToString(" ")}\": ${e.message}")
            null
        }
    }
}
