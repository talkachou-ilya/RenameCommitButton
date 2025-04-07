package com.github.talkachouilya.renamecommitbutton

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

class RenameCommitService(private val project: Project) {
    private val repository: GitRepository
    private val commit: GitCommit

    init {
        repository = getRepository()
        commit = getCurrentCommit()
    }

    private fun getRepository(): GitRepository {
        val projectRoot = project.guessProjectDir()
            ?: throw RenameCommitException("No project base dir")

        return getRepositoryManager(project)
            .getRepositoryForRootQuick(projectRoot)
            ?: throw RenameCommitException("No repository")
    }

    private fun getCurrentCommit(): GitCommit {
        if (repository.currentRevision == null) {
            throw RenameCommitException("No HEAD")
        }

        return GitHistoryUtils.history(project, repository.root, HEAD)
            .firstOrNull()
            ?: throw RenameCommitException("No commit")
    }

    fun renameCommit(newCommitName: String) {
        val handler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        handler.addParameters("--amend", "-m", newCommitName)

        Git.getInstance().runCommand(handler).getOutputOrThrow()

        VcsDirtyScopeManager.getInstance(project).markEverythingDirty()
    }

    fun getCommitMessage(): String {
        return commit.subject
    }
}