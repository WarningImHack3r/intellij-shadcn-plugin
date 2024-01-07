package com.github.warningimhack3r.intellijshadcnplugin.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

class NotificationManager(val project: Project? = null) {

    private fun createNotification(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: List<NotificationAction> = emptyList()
    ) =  Notification(
            "shadcn/ui",
            title,
            content,
            type
        ).apply {
            actions.forEach { addAction(it) }
        }

    @Suppress("UnstableApiUsage") // notifyAndHide is still experimental
    private fun sendNotification(
        notification: Notification,
        hide: Boolean = false
    ) {
        if (hide) {
            project?.let {
                Notifications.Bus.notifyAndHide(notification, it)
            } ?: Notifications.Bus.notifyAndHide(notification)
        } else {
            project?.let {
                Notifications.Bus.notify(notification, it)
            } ?: Notifications.Bus.notify(notification)
        }
    }

    fun sendNotification(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: (Notification) -> List<NotificationAction> = { emptyList() }
    ) = sendNotification(
        createNotification(title, content, type).apply { actions(this).forEach { addAction(it) } },
    )

    fun sendNotificationAndHide(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: (Notification) -> List<NotificationAction> = { emptyList() }
    ) = sendNotification(
        createNotification(title, content, type).apply { actions(this).forEach { addAction(it) } },
        true
    )
}
