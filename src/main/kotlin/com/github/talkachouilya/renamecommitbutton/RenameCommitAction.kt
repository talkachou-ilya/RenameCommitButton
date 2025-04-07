package com.github.talkachouilya.renamecommitbutton

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager
import git4idea.GitCommit
import git4idea.GitUtil.HEAD
import git4idea.GitUtil.getRepositoryManager
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import com.intellij.notification.Notifications.Bus.notify as busNotify

class RenameCommitAction : com.intellij.openapi.actionSystem.AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        when (val data = collectData(e)) {
            is Data.WithCommit -> {
                val dialog = RenameCommitDialog(data.commit.subject)

                if (dialog.showAndGet()) {
                    renameCurrentCommit(data.project, data.repository, dialog.getCommitName())
                }
            }

            is Data.Disabled -> println(data.description)
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        when (val data = collectData(e)) {
            is Data.Disabled -> {
                e.presentation.isVisible = true
                e.presentation.isEnabled = false
                e.presentation.description = data.description
            }

            else -> e.presentation.isEnabledAndVisible = true
        }
    }

    private sealed class Data {
        class Disabled(val description: String) : Data()
        class WithCommit(val project: Project, val repository: GitRepository, val commit: GitCommit) : Data()
    }

    private fun collectData(e: AnActionEvent): Data {
        val project = e.project ?: return Data.Disabled("No project")
        val projectRoot = project.guessProjectDir() ?: return Data.Disabled("No project base dir")

        val repository = getRepositoryManager(project).getRepositoryForRootQuick(projectRoot)
            ?: return Data.Disabled("No repository")

        if (repository.currentRevision == null) return Data.Disabled("No HEAD")

        val commit =
            GitHistoryUtils.history(project, repository.root, HEAD).firstOrNull() ?: return Data.Disabled("No commit")

        return Data.WithCommit(project, repository, commit)
    }

    private fun renameCurrentCommit(
        project: Project, repository: GitRepository, newCommitName: String
    ) {
        val handler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        handler.addParameters("--amend", "-m", newCommitName)

        try {
            Git.getInstance().runCommand(handler).getOutputOrThrow()
            notify(
                "Commit has been successfully renamed",
                "Commit message reworded to \"$newCommitName\"",
                NotificationType.INFORMATION
            )
            VcsDirtyScopeManager.getInstance(project).markEverythingDirty()
        } catch (e: Exception) {
            notify(
                "Error renaming commit",
                e.message ?: "Unknown error",
                NotificationType.ERROR
            )
        }
    }

    private fun notify(title: String, message: String, type: NotificationType) {
        busNotify(Notification("GitCommitRename", title, message, type))
    }
}