package com.github.warningimhack3r.intellijshadcnplugin.backend.sources

import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class UnparseableConfigException(
    project: Project,
    configName: String,
    cause: Throwable? = null
) : Exception("Unable to parse $configName", cause) {
    init {
        NotificationManager(project).sendNotification(
            "Unparseable configuration file",
            "Your <code>${configName}</code> file could not be parsed.<br />Please check that it is a valid JSON and that it contains the correct fields.",
            NotificationType.ERROR
        )
    }
}
