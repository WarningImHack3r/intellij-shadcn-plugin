package com.github.warningimhack3r.intellijshadcnplugin.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.TimeUnit

class NotificationManager(val project: Project? = null) {

    private fun createNotification(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: List<NotificationAction> = emptyList()
    ) = Notification(
        "shadcn/ui", title, content, type
    ).apply {
        actions.forEach { addAction(it) }
    }

    private fun sendNotification(
        notification: Notification,
        hide: Boolean = false
    ) {
        project?.let {
            Notifications.Bus.notify(notification, it)
        } ?: Notifications.Bus.notify(notification)
        if (hide) {
            // Taken from experimental Notifications.Bus.notifyAndHide
            AppExecutorUtil.getAppScheduledExecutorService().schedule({
                notification.expire()
            }, 5, TimeUnit.SECONDS)
        }
    }

    fun sendNotification(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: (Notification) -> List<NotificationAction> = { emptyList() }
    ) = sendNotification(
        createNotification(title, content, type).apply {
            actions(this).forEach { action ->
                addAction(action)
            }
        },
    )

    fun sendNotificationAndHide(
        title: String,
        content: String,
        type: NotificationType = NotificationType.INFORMATION,
        actions: (Notification) -> List<NotificationAction> = { emptyList() }
    ) = sendNotification(
        createNotification(title, content, type).apply {
            actions(this).forEach { action ->
                addAction(action)
            }
        }, true
    )
}
