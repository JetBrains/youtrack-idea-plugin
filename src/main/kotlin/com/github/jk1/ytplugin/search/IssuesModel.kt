package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.project.Project
import com.intellij.util.containers.SortedList
import java.util.*
import javax.swing.AbstractListModel

class IssuesModel(val project: Project, val viewer: IssueViewer) : AbstractListModel<Issue>() {

    private val myIssues = SortedList(Comparator<Issue> { o1, o2 ->
        o1.id.compareTo(o2.id)
    })

    //private val myStorage = Storage(project).getInstance(project)

    init{

    }


    override fun getElementAt(index: Int): Issue? {
        return myIssues[index]
    }

    override fun getSize(): Int {
        return myIssues.size
    }
}