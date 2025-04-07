package com.github.talkachouilya.renamecommitbutton

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent


class RenameCommitDialog(prevCommitName: String) : DialogWrapper(true) {

    private var newCommitName: String = prevCommitName

    init {
        title = "Rename Commit"
        setOKButtonText("Rename")
        setCancelButtonText("Cancel")
        init()
    }


    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("New commit name:")
            }
            row {
                textField()
                    .focused()
                    .resizableColumn()
                    .bindText(::newCommitName)
                    .validationOnApply {
                        if (it.text.isEmpty()) {
                            error("Please enter a commit name")
                        } else {
                            null
                        }
                    }
                    .applyToComponent {
                        minimumSize = Dimension(340, 29)
                    }
            }
        }
    }

    fun getCommitName(): String {
        return newCommitName
    }
}