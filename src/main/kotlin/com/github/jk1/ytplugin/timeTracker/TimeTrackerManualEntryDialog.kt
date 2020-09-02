package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.TaskManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import org.jdesktop.swingx.JXDatePicker
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*


open class TimeTrackerManualEntryDialog(override val project: Project, val repo: YouTrackServer) : DialogWrapper(project, false), ComponentAware {

    var state = 0

    private var dateLabel = JBLabel("Date:")
    private val datePicker = JXDatePicker()
    private var typeComboBox =  JComboBox(arrayOf<String>("Development"))

    private var timeLabel = JBLabel("Time:")
    private lateinit var timeTextField: JBTextField

    private lateinit var commentPanel: JPanel

    private lateinit var typePanel: JPanel
    private lateinit var parametersPanel: JPanel

    private var commentLabel= JBLabel("Comment:")
    private var typeLabel = JBLabel("Work item type:")
    private lateinit var commentTextField: JBTextField

    private var notifier = JBLabel("" )


    init {
        title = "Create new work item"
        super.init()
    }

    override fun show() {
        init()
        super.show()
    }

    fun prepareMainPane() : JPanel{

        val picker = JXDatePicker()
        picker.date = Calendar.getInstance().time
        picker.setFormats(SimpleDateFormat("dd.MM.yyyy"))

        val timePanel = JPanel(FlowLayout(2))
        val datePanel = JPanel(FlowLayout(2))

        datePanel.add(dateLabel)
        datePanel.add(datePicker)

        timeTextField = JBTextField("1h 30m")
        commentTextField = JBTextField("")
        //TODO
        commentTextField.preferredSize = Dimension(347, 32)

        // displayPanel.add( titleText, BorderLayout.NORTH );
        timePanel.add(timeLabel)
        timePanel.add(timeTextField)

        parametersPanel = JPanel(FlowLayout(2))
        parametersPanel.add(datePanel)

        parametersPanel.add(timePanel)

        commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        val types = mutableListOf<String>()
        TimeTrackerRestClient(repo).getAvailableWorkItems().map { types.add(it.name) }
        for (t in types)
        typeComboBox =  JComboBox(types.toTypedArray())
        typeComboBox.selectedIndex = 0
        typeComboBox.isEditable = true

        typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        return JPanel().apply {
            layout = VerticalLayout(3)
            add(parametersPanel)
            add(typePanel)
            add(commentPanel)
            add(notifier)
        }
    }

    override fun createActions(): Array<out Action> =
            arrayOf(OkAction("Ok"), cancelAction)

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
        button.actionMap.put("apply", action)
        return button
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(GridLayout())
        val mainPane = prepareMainPane()
        contextPane.apply {
            preferredSize = Dimension(440, 180)
            minimumSize = preferredSize
            add(mainPane)
        }
        return contextPane
    }

    fun formatTime(inputTime: String) : String {
        var weeks: Long = 0
        var days: Long = 0
        var hours: Long = 0
        var minutes: Long = 0

        var time = inputTime.replace("\\s".toRegex(), "")

        var sepPos: Int = time.lastIndexOf("w")
        if (sepPos != -1){
            weeks = time.substring(0, sepPos).toLong()
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("d")
        if (sepPos != -1){
            days = time.substring(0, sepPos).toLong()
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("h")
        if (sepPos != -1){
            hours= time.substring(0, sepPos).toLong()
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("m")
        if (sepPos != -1){
            minutes = time.substring(0, sepPos).toLong()
        }

        return (weeks * 10080 + days * 1440 + hours * 60 + minutes).toString()
    }

    inner class OkAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            var status = 0
            var taskManager = TaskManager.getManager(project)
            val activeTask = taskManager.activeTask

            val sdf = SimpleDateFormat("yyyy-MM-dd")
            val date = sdf.parse(datePicker.date.toInstant().toString().substring(0, 10))

            typeComboBox.getItemAt(typeComboBox.selectedIndex)?.let {
                status = TimeTrackerRestClient(repo).postNewWorkItem(activeTask.id,
                        formatTime(timeTextField.text), it, commentTextField.text, date.time.toString())
            }
            state = status
            if (status == 200) {
                this@TimeTrackerManualEntryDialog.close(0)
            }
            else {
                notifier.foreground = Color.red
                notifier.text = " Could not post, please check your input"
            }
        }
    }

}

