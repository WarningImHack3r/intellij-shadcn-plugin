package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.IOException
import java.nio.file.NoSuchFileException

@Service(Service.Level.PROJECT)
class FileManager(private val project: Project) {
    companion object {
        private val log = logger<FileManager>()

        @JvmStatic
        fun getInstance(project: Project): FileManager = project.service()
    }

    fun saveFileAtPath(file: PsiFile, path: String) {
        var deepest = getDeepestFileForPath(path)
        val deepestRelativePath = deepest.path.substringAfter("${project.basePath!!}/")
        path.substringAfter(deepestRelativePath).split('/').filterNot { it.isEmpty() }.also {
            log.debug("Creating subdirectories ${it.joinToString(", ")}")
        }.forEach { subdirectory ->
            deepest = runWriteAction { deepest.createChildDirectory(this, subdirectory) }
        }
        runWriteAction {
            deepest.createChildData(this, file.name).setBinaryContent(file.text.toByteArray())
        }.also {
            log.debug("Saved file ${file.name} under ${deepest.path}")
        }
    }

    fun deleteFile(file: VirtualFile): Boolean {
        return try {
            runWriteAction { file.delete(this) }.let { true }
        } catch (_: IOException) {
            false
        }.also {
            if (!it) log.warn("Unable to delete file at path ${file.path}")
            else log.debug("Deleted file at path ${file.path}")
        }
    }

    fun deleteFileAtPath(path: String): Boolean {
        return getFileAtPath(path)?.let { deleteFile(it) } ?: false.also {
            log.warn("No file to delete found at path $path")
        }
    }

    fun getVirtualFilesByName(name: String): Collection<VirtualFile> {
        return (if (name.startsWith('.')) {
            log.debug("Using dotfiles method to find files named $name")
            // For some reason, dotfiles/folders don't show up with
            // a simple call to [FilenameIndex.getVirtualFilesByName].
            // This is a dirty workaround to make it work on production,
            // because it works fine during local development.
            runReadAction {
                FilenameIndex.getVirtualFilesByName(
                    "components.json",
                    GlobalSearchScope.projectScope(project)
                ) + FilenameIndex.getVirtualFilesByName(
                    "ui.config.json",
                    GlobalSearchScope.projectScope(project)
                )
            }.flatMap { it.parent.children.toList() }.toSet().filter {
                it.name.contains(name)
            }.ifEmpty {
                log.warn("No file named $name found with the workaround")
                emptyList<VirtualFile>()
            }
        } else {
            runReadAction {
                FilenameIndex.getVirtualFilesByName(
                    name,
                    GlobalSearchScope.projectScope(project)
                )
            }
        }).filter { file ->
            !file.path.contains("/node_modules/")
                    && !file.path.contains("/.git/")
                    && !file.parent.name.startsWith('.')
        }.sortedBy { file ->
            name.toRegex().find(file.path)?.range?.first ?: Int.MAX_VALUE
        }.also {
            log.debug("Found ${it.size} file(s) named $name: $it")
        }
    }

    private fun getDeepestFileForPath(filePath: String): VirtualFile {
        var paths = filePath.split('/')
        var currentFile = getVirtualFilesByName(paths.first()).firstOrNull()
            ?: throw NoSuchFileException("No file found at path $filePath")
        paths = paths.drop(1)
        for (path in paths) {
            val child = currentFile.findChild(path)
            if (child == null) {
                break
            } else {
                currentFile = child
            }
        }
        return currentFile.also {
            log.debug("Found deepest file for path $filePath: ${it.path}")
        }
    }

    fun getFileAtPath(filePath: String): VirtualFile? {
        return try {
            val deepest = getDeepestFileForPath(filePath)
            if (deepest.name == filePath.substringAfterLast('/')) deepest else null
        } catch (_: Exception) {
            null
        }.also {
            if (it == null) log.warn("No file found at path $filePath")
            else log.debug("Found file at path $filePath: ${it.path}")
        }
    }

    fun getFileContentsAtPath(path: String): String? {
        return getFileAtPath(path)?.contentsToByteArray()?.decodeToString()
    }
}
