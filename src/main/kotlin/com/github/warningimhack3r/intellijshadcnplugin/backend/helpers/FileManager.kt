package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.NoSuchFileException

class FileManager(private val project: Project) {
    fun saveFileAtPath(file: PsiFile, path: String) {
        var deepest = getDeepestFileForPath(path)
        val deepestRelativePath = deepest.path.substringAfter("${project.basePath!!}/")
        path.substringAfter(deepestRelativePath).split('/').filterNot { it.isEmpty() }.forEach { subdirectory ->
            deepest = deepest.createChildDirectory(this, subdirectory)
        }
        deepest.createChildData(this, file.name).apply {
            setBinaryContent(file.text.toByteArray())
        }
    }

    fun deleteFileAtPath(path: String): Boolean {
        return getFileAtPath(path)?.delete(this)?.let { true } ?: false
    }

    fun getVirtualFilesByName(name: String): Collection<VirtualFile> {
        return FilenameIndex.getVirtualFilesByName(
            name,
            GlobalSearchScope.projectScope(project)
        ).filter { file ->
            val nodeModule = file.path.contains("node_modules")
            if (!name.startsWith(".")) {
                !nodeModule && !file.path.substringAfter(project.basePath!!).startsWith(".")
            } else !nodeModule
        }
    }

    private fun getDeepestFileForPath(filePath: String): VirtualFile {
        var paths = filePath.split('/')
        var currentFile = getVirtualFilesByName(paths.first()).firstOrNull() ?: throw NoSuchFileException("No file found at path $filePath")
        paths = paths.drop(1)
        for (path in paths) {
            val child = currentFile.findChild(path)
            if (child == null) {
                return currentFile
            } else {
                currentFile = child
            }
        }
        return currentFile
    }

    fun getFileAtPath(filePath: String): VirtualFile? {
        try {
            val deepest = getDeepestFileForPath(filePath)
            return if (deepest.name == filePath.substringAfterLast('/')) deepest else null
        } catch (e: Exception) {
            return null
        }
    }

    fun getFileContentsAtPath(path: String): String? {
        return getFileAtPath(path)?.contentsToByteArray()?.decodeToString()
    }
}
