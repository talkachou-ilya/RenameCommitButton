package com.github.talkachouilya.renamecommitbutton

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.notification.Notifications.Bus.notify as busNotify

class RenameCommitAction : com.intellij.openapi.actionSystem.AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        try {
            val service = RenameCommitService(project)

            val oldCommitMessage = service.getCommitMessage()

            val dialog = RenameCommitDialog(oldCommitMessage)

            if (dialog.showAndGet()) {
                val newCommitName = dialog.getCommitName()
                service.renameCommit(newCommitName)
                notifySuccess("Commit message reworded to \"$newCommitName\"")
            }
        } catch (e: Exception) {
            notifyError(e.message ?: "Unknown error")
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)

        val project = e.project

        if (project == null) {
            disableAction(e, "No project")
            return
        }

        try {
            RenameCommitService(project)
            e.presentation.isEnabledAndVisible = true
        } catch (exception: RenameCommitException) {
            disableAction(e, exception.message ?: "Unknown error")
        }
    }

    private fun disableAction(e: AnActionEvent, message: String) {
        e.presentation.isVisible = true
        e.presentation.isEnabled = false
        e.presentation.description = message
    }

    private fun notifyError(message: String) {
        busNotify(
            Notification(
                "GitCommitRename",
                "Error renaming commit",
                message,
                NotificationType.ERROR
            )
        )
    }

    private fun notifySuccess(message: String) {
        busNotify(
            Notification(
                "GitCommitRename",
                "Commit has been successfully renamed",
                message,
                NotificationType.INFORMATION
            )
        )
    }
}