package com.github.warningimhack3r.intellijshadcnplugin.backend.helpers

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory

object PsiHelper {

    fun createPsiFile(project: Project, fileName: String, text: String): PsiFile {
        assert(fileName.contains('.')) { "File name must contain an extension" }
        return PsiFileFactory.getInstance(project).createFileFromText(
            fileName, FileTypeManager.getInstance().getFileTypeByExtension(
                fileName.substringAfterLast('.')
            ), text
        )
    }

    fun createPsiFile(project: Project, language: FileType, text: String): PsiFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            "temp.${language.defaultExtension}", language, text
        )
    }

    fun writeAction(file: PsiFile, description: String? = null, action: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(
            file.project, description,
            "com.github.warningimhack3r.intellijshadcnplugin",
            action, file
        )
    }
}
