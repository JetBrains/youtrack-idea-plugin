package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.issues.actions.IssueActionGroup
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiDocumentManager
import com.intellij.tasks.youtrack.YouTrackIntellisense.INTELLISENSE_KEY
import com.intellij.tasks.youtrack.lang.YouTrackLanguage
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.KeyStroke

class IssueSearchBar(val server: YouTrackServer) : JPanel(BorderLayout()) {

    private val project = server.project
    private val searchField = LanguageTextField(YouTrackLanguage.INSTANCE, project, server.defaultSearch)
    private val actionGroup = IssueActionGroup(searchField)

    var actionListener = { _: String -> }

    init {
        searchField.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        searchField.setPlaceholder("Empty search query - all issues will be displayed")
        actionGroup.add(SearchIssueAnAction())
        add(searchField, BorderLayout.CENTER)
        add(actionGroup.createHorizontalToolbarComponent(), BorderLayout.EAST)
        // completion support
        val file = PsiDocumentManager.getInstance(project).getPsiFile(searchField.document)
        file?.putUserData(INTELLISENSE_KEY, server.getSearchCompletionProvider())
        // key bindings
        // todo: find a better way to attach onEnter handler to LanguageTextField
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent?) {
                val component = searchField.editor!!.contentComponent
                component.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
                component.actionMap.put("apply", SearchIssueSwingAction())
            }
        })
        border = BorderFactory.createEmptyBorder(0, 0, 0, -15)
    }

    inner class SearchIssueSwingAction : AbstractAction() {
        override fun actionPerformed(event: ActionEvent) {
            actionListener.invoke(searchField.text)
        }
    }

    inner class SearchIssueAnAction : AnAction(), DumbAware {

        init {
            templatePresentation.description = "Filter issues with YouTrack search query syntax"
            templatePresentation.text = "Search"
            templatePresentation.icon = AllIcons.Actions.Find
        }

        override fun actionPerformed(e: AnActionEvent?) {
            actionListener.invoke(searchField.text)
        }
    }
}