package com.github.jk1.ytplugin.setupWindow

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.Configurable.WithEpDependencies
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.actions.IconWithTextAction
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.tasks.*
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.config.TaskRepositoriesConfigurable
import com.intellij.tasks.config.TaskRepositoryEditor
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.Consumer
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.ContainerUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.util.*
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JPanel

// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/**
 * @author Dmitry Avdeev
 */
class YouTrackRepositoriesConfigurable(private val myProject: Project) : NoScroll, SearchableConfigurable, WithEpDependencies {
    private lateinit var myPanel: JPanel
    private var myServersPanel: JPanel = JPanel()
    private var myRepositoriesList: JBList<TaskRepository?>
    private lateinit var myToolbarPanel: JPanel
    private lateinit var myRepositoryEditor: JPanel
    private var myServersLabel: JBLabel = JBLabel()
    private lateinit var mySplitter: Splitter
    private lateinit var myEmptyPanel: JPanel
    private var myRepositories: MutableList<TaskRepository?> = ArrayList()
    private var myEditors: MutableList<TaskRepositoryEditor> = ArrayList()
    private var myChangeListener: Consumer<TaskRepository>
    private var count = 0
    private var myRepoNames: MutableMap<TaskRepository, String> = ConcurrentFactoryMap.createMap { repository: TaskRepository? -> Integer.toString(count++) }
    private var myManager: TaskManagerImpl
    private fun addRepository(repository: TaskRepository) {
        this.myRepositories.add(repository);
        val mod = myRepositoriesList.model as CollectionListModel
        mod.add((repository))
        addRepositoryEditor(repository)
        myRepositoriesList.selectedIndex = myRepositoriesList.model.size - 1
    }

    private fun addRepositoryEditor(repository: TaskRepository?) {
        val editor = repository!!.repositoryType.createEditor(repository, myProject, myChangeListener)
        myEditors.add(editor)
        val component = editor.createComponent()
        val name = myRepoNames[repository]
        myRepositoryEditor!!.add(component, name)
        myRepositoryEditor.doLayout()
    }

    private val selectedRepository: TaskRepository?
        private get() = myRepositoriesList.selectedValue

    override fun getDisplayName(): String {
        return TaskBundle.message("configurable.TaskRepositoriesConfigurable.display.name")
    }

    override fun getHelpTopic(): String? {
        return "reference.settings.project.tasks.servers"
    }

    override fun createComponent(): JComponent? {
        return myPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return myRepositoriesList
    }

    override fun isModified(): Boolean {
        return myRepositories != reps
    }

    override fun apply() {
        val newRepositories = ContainerUtil.map(myRepositories) { obj: TaskRepository? -> obj!!.clone() }
        myManager.setRepositories(newRepositories)
        myManager.updateIssues(null)
        RecentTaskRepositories.getInstance().addRepositories(myRepositories)
    }

    override fun reset() {
        myRepoNames.clear()
        myRepositoryEditor.removeAll()
        myRepositoryEditor.add(myEmptyPanel as JComponent, "empty.panel");

//        myRepositoryEditor.add(myEmptyPanel, TaskRepositoriesConfigurable.Companion.EMPTY_PANEL)
        //    ((CardLayout)myRepositoryEditor.getLayout()).show(myRepositoryEditor, );
        myRepositories.clear()
        val listModel: CollectionListModel<TaskRepository> = CollectionListModel(ArrayList())
        for (repository in myManager.allRepositories) {
            val clone = repository.clone()
            assert(clone == repository) { repository.javaClass.name }
            myRepositories.add(clone)
            listModel.add(clone)
        }

        myRepositoriesList.model = listModel
        for (clone in myRepositories) {
            addRepositoryEditor(clone)
        }
        if (!myRepositories.isEmpty()) {
            myRepositoriesList.setSelectedValue(myRepositories[0], true)
        }
    }

    private val reps: List<TaskRepository>
        get() = Arrays.asList(*myManager.allRepositories)

    override fun disposeUIResources() {
        for (editor in myEditors) {
            Disposer.dispose(editor)
        }
    }

    override fun getId(): String {
        return TaskRepositoriesConfigurable.ID
    }

    override fun enableSearch(option: String): Runnable? {
        val matched = myRepositories.stream().filter { repository: TaskRepository? -> repository!!.repositoryType.name.contains(option) }.findFirst().orElse(null)
        return if (matched == null) null else Runnable { myRepositoriesList.setSelectedValue(matched, true) }
    }

    override fun getDependencies(): Collection<BaseExtensionPointName<*>> {
        return listOf(TaskRepositoryType.EP_NAME)
    }

    private abstract inner class AddServerAction : IconWithTextAction, DumbAware {
        internal constructor(subtype: TaskRepositorySubtype) : super(Supplier<String> { subtype.name }, TaskBundle.messagePointer("settings.new.server", subtype.name), subtype.icon) {}
        internal constructor(repository: TaskRepository) : super(repository.url, repository.url, repository.icon) {}

        protected abstract val repository: TaskRepository

        override fun actionPerformed(e: AnActionEvent) {
            addRepository(repository)
        }
    }

    companion object {
        const val ID = "tasks.servers"
        private const val EMPTY_PANEL = "empty.panel"
    }

    init {
        myManager = TaskManager.getManager(myProject) as TaskManagerImpl
        myRepositoriesList = JBList<TaskRepository?>()
        myRepositoriesList.emptyText.text = TaskBundle.message("settings.no.servers")
        myServersLabel.labelFor = myRepositoriesList
        myServersPanel.minimumSize = Dimension(-1, 100)
        val repositoryType: YouTrackRepositoryType = YouTrackRepositoryType()
        val createActions: MutableList<AnAction> = ArrayList()
        for (subtype in repositoryType.availableSubtypes as List<TaskRepositorySubtype>) {
                createActions.add(object : AddServerAction(subtype) {
                    override val repository: TaskRepository
                        get() = repositoryType.createRepository(subtype)
                })
        }

        val toolbarDecorator = ToolbarDecorator.createDecorator(myRepositoriesList).disableUpDownActions()
        toolbarDecorator.setAddAction { anActionButton ->
            val group = DefaultActionGroup()
            for (aMyAdditional in createActions) {
                group.add(aMyAdditional)
            }
            val repositories = RecentTaskRepositories.getInstance().repositories
            repositories.removeAll(myRepositories)
            if (!repositories.isEmpty()) {
                group.add(Separator.getInstance())
                for (repository in repositories) {
                    group.add(object : AddServerAction(repository) {
                        override val repository: TaskRepository
                            protected get() = repository
                    })
                }
            }
            JBPopupFactory.getInstance()
                    .createActionGroupPopup("Add Server", group, DataManager.getInstance().getDataContext(anActionButton.contextComponent),
                            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true).show(
                            anActionButton.preferredPopupPoint!!)
        }
        toolbarDecorator.setRemoveAction {
            val repository = selectedRepository
            if (repository != null) {
                val model = myRepositoriesList.model as CollectionListModel
                model.remove(repository)
                myRepositories.remove(repository)
                if (model.size > 0) {
                    myRepositoriesList.setSelectedValue(model.getElementAt(0), true)
                } else {
                    myRepositoryEditor!!.removeAll()
                    myRepositoryEditor.repaint()
                }
            }
        }
        myServersPanel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER)
        myRepositoriesList.selectionModel.addListSelectionListener {
            val repository = selectedRepository
            if (repository != null) {
                val name = myRepoNames[repository]!!
                (myRepositoryEditor.layout as CardLayout).show(myRepositoryEditor, name)
                mySplitter.doLayout()
                mySplitter.repaint()
            }
        }
        myRepositoriesList.setCellRenderer(SimpleListCellRenderer.create { label: JBLabel, value: TaskRepository?, index: Int ->
            label.icon = value!!.icon
            label.text = value.presentableName
        })
        myChangeListener = Consumer { repository: TaskRepository? ->
            if (repository != null) {
                (myRepositoriesList.model as CollectionListModel).contentsChanged(repository)
            }
        }
    }
}