package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.IOException
import java.nio.file.NoSuchFileException

class FileManager(private val project: Project) {
    private val log = logger<FileManager>()

    fun saveFileAtPath(file: PsiFile, path: String) {
        var deepest = getDeepestFileForPath(path)
        val deepestRelativePath = deepest.path.substringAfter("${project.basePath!!}/")
        path.substringAfter(deepestRelativePath).split('/').filterNot { it.isEmpty() }.also {
            log.debug("Creating subdirectories ${it.joinToString(", ")}")
        }.forEach { subdirectory ->
            deepest = deepest.createChildDirectory(this, subdirectory)
        }
        deepest.createChildData(this, file.name).setBinaryContent(file.text.toByteArray()).also {
            log.debug("Saved file ${file.name} under ${deepest.path}")
        }
    }

    fun deleteFileAtPath(path: String): Boolean {
        return try {
            getFileAtPath(path)?.delete(this)?.let { true } ?: false
        } catch (e: IOException) {
            false
        }.also {
            if (!it) log.warn("Unable to delete file at path $path")
            else log.debug("Deleted file at path $path")
        }
    }

    fun getVirtualFilesByName(name: String): Collection<VirtualFile> {
        return (if (name.startsWith('.')) {
            log.debug("Using workaround to find files named $name")
            // For some reason, dotfiles/folders don't show up with
            // a simple call to FilenameIndex.getVirtualFilesByName.
            // This is a dirty workaround to make it work on production,
            // because it works fine during local development.
            FilenameIndex.getVirtualFilesByName(
                "components.json",
                GlobalSearchScope.projectScope(project)
            ).firstOrNull().also {
                if (it == null) {
                    log.warn("components.json not found with the workaround")
                }
            }?.parent?.children?.filter {
                it.name.contains(name)
            } ?: listOf<VirtualFile?>().also {
                log.warn("No file named $name found with the workaround")
            }
        } else {
            FilenameIndex.getVirtualFilesByName(
                name,
                GlobalSearchScope.projectScope(project)
            )
        }).sortedBy { file ->
            name.toRegex().find(file.path)?.range?.first ?: Int.MAX_VALUE
        }.also {
            log.debug("Found ${it.size} files named $name: ${it.toList()}")
        }
    }

    private fun getDeepestFileForPath(filePath: String): VirtualFile {
        var paths = filePath.split('/')
        var currentFile = getVirtualFilesByName(paths.first()).firstOrNull() ?: throw NoSuchFileException("No file found at path $filePath").also {
            log.warn("No file found at path ${paths.first()}")
        }
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
        } catch (e: Exception) {
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
