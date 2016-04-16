package com.github.jk1.ytplugin.search

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.ListModel
import javax.swing.event.ListDataListener

/**
 * Created by elle on 27.03.16.
 */
class IssueViewer(val project: Project, parent: Disposable) : JBLoadingPanel(BorderLayout(), parent), DataProvider, Disposable {

    var myList: JBList = JBList()

    init {
        myList.fixedCellHeight = 80
        myList.cellRenderer = IssueListCellRenderer()
        myList.model = CollectionListModel<Issue>(Issue("id", "summary", "description"))

        val splitter = EditorSplitter(project)

        val browser = MessageBrowser(project)

        val scrollPane = JBScrollPane(myList, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        scrollPane.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                myList.fixedCellWidth = scrollPane.visibleRect.width - 30
            }
        })

        splitter.firstComponent = scrollPane
        splitter.secondComponent = browser

        add(splitter, BorderLayout.CENTER)

        startLoading()
        ApplicationManager.getApplication().executeOnPooledThread {
            val model = Ref.create(myList.model)
            try {
                model.set(IssuesModel(project, this))
            } finally {
                ApplicationManager.getApplication().invokeLater {
                    myList.model = model.get()
                    stopLoading()
                }
            }
        }

    }

    override fun getData(dataId: String): Any? {
        if (PlatformDataKeys.PROJECT.equals(dataId)) {
            return project
        }
        if (DataKey.create<Array<Issue>>("MYY_ISSUES_ARRAY").equals(dataId)) {
            val values = myList.selectedValues
            val issues = arrayOfNulls<Issue>(values.size)
            for (i in values.indices) {
                issues[i] = values[i] as Issue
            }
            return issues
        }

        return null
    }

    override fun dispose() {

    }
}