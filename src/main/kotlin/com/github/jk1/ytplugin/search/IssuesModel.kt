package com.github.jk1.ytplugin.search

import com.intellij.openapi.project.Project
import com.intellij.util.containers.SortedList
import java.util.*
import javax.swing.AbstractListModel

/**
 * Created by elle on 03.04.16.
 */
class IssuesModel(val project: Project, val viewer: IssueViewer) : AbstractListModel<Issue>() {

    private val myIssues = SortedList(Comparator<Issue> { o1, o2 ->
        o1.id.compareTo(o2.id)
    })

    //private val myStorage = Storage(project).getInstance(project)

    init{
        myIssues.add(Issue("1","summary","description"))
        myIssues.add(Issue("2","summary","description"))
        myIssues.add(Issue("3","summary","description"))
        myIssues.add(Issue("4","summary","description"))
        myIssues.add(Issue("5","summary","description"))
        myIssues.add(Issue("6","summary","description"))
    }


    override fun getElementAt(index: Int): Issue? {
        return myIssues[index]
    }

    override fun getSize(): Int {
        return myIssues.size
    }
}