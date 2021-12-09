package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.TaskManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.michaelbaranov.microba.calendar.DatePicker
import org.apache.http.HttpStatus
import org.jdesktop.swingx.JXDatePicker
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import javax.swing.*

class TimeTrackerManualEntryDialog(override val project: Project, val repo: YouTrackServer) : DialogWrapper(project, false), ComponentAware {

    private var dateLabel = JBLabel("Date:")
    private val datePicker = DatePicker(Date())

    private var idLabel = JBLabel("Issue:   ")

    private var idComboBox = ComboBox(arrayOf<String>())
    private var typeComboBox = ComboBox(arrayOf("Development"))
    private var timeLabel = JBLabel("Spent time:")

    private lateinit var hoursSpinner: JSpinner
    private lateinit var minutesSpinner: JSpinner

    private val okButton = JButton("Save")
    private val cancelButton = JButton("Cancel")

    private var commentLabel = JBLabel("Comment:")
    private var typeLabel = JBLabel("Work type:")
    private lateinit var commentTextField: JBTextField

    private var notifier = JBLabel("")
    private val ids = issueStoreComponent[repo].getAllIssues()
    private val tasksIdRepresentation = mutableListOf<String>()
    private val tasksIds = mutableListOf<String>()

    private val labelsMargin = 124

    init {
        title = "Add Spent Time"
        rootPane.defaultButton = okButton
    }

    override fun show() {
        init()
        super.show()
    }

    private fun prepareMainPane(): JPanel {

        val idPanel = createIdPanel()

        val timePanel = createTimePanel()

        val datePanel = createDatePanel()

        val commentPanel = createCommentPanel()

        val typePanel = createTypePanel()

        val notifierPanel = createNotifierPanel()

        val buttonsPanel = createButtonsPanel()
        return JPanel().apply {
            layout = VerticalLayout(8)
            setResizable(false)
            add(timePanel)
            add(idPanel)
            add(typePanel)
            add(commentPanel)
            add(datePanel)
            add(notifierPanel)
            add(buttonsPanel)
        }
    }

    private fun createNotifierPanel(): JPanel {
        val notifierPanel = JPanel(FlowLayout(2))

        val notifierLabel = JLabel("")
        notifierLabel.preferredSize = Dimension(labelsMargin, 30)
        notifierPanel.add(notifierLabel)
        notifierPanel.add(notifier)

        return notifierPanel
    }

    private fun createTypePanel(): JPanel {

        val timerService = TimeTrackingConfigurator()
        val future = ApplicationManager.getApplication().executeOnPooledThread(
                Callable {
                    timerService.getAvailableWorkItemsTypes(repo)
                })

        val types = future.get()
        val timer = project.let { timeTrackerComponent }
        typeComboBox = ComboBox(types.toTypedArray())
        var idx = 0

        try {
            typeComboBox.selectedIndex = 0
            if (types.isNotEmpty()) {
                types.mapIndexed { index, value ->
                    if (value == timer.type) {
                        idx = index
                    }
                }
                typeComboBox.selectedIndex = idx
            }
        } catch (e: IllegalArgumentException) {
            typeComboBox.selectedIndex = -1
            logger.warn("Failed to fetch work items types")
        }
        typeComboBox.isEditable = true

        val typePanel = JPanel(FlowLayout(2))
        typeLabel.preferredSize = Dimension(labelsMargin, 30)
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        return typePanel
    }

    private fun createIdPanel(): JPanel {
        val picker = JXDatePicker()
        picker.isEditable = true
        picker.date = Calendar.getInstance().time
        picker.setFormats(SimpleDateFormat("dd.MM.yyyy"))

        val idPanel = JPanel(FlowLayout(2))
        for (id in ids) {
            tasksIdRepresentation.add(id.issueId + ": " + id.summary)
            tasksIds.add(id.issueId)
        }

        idComboBox = ComboBox(tasksIdRepresentation.toTypedArray())
        idComboBox.preferredSize = Dimension(390, typeComboBox.preferredSize.height)
        idComboBox.selectedIndex = tasksIds.indexOf(TaskManager.getManager(project).activeTask.id)

        idLabel.preferredSize = Dimension(labelsMargin, 30)

        idPanel.add(idLabel)
        idPanel.add(idComboBox)

        return idPanel
    }

    private fun createDatePanel(): JPanel {
        val datePanel = JPanel(FlowLayout(0))
        dateLabel.preferredSize = Dimension(labelsMargin, 30)
        datePicker.preferredSize = Dimension(200, 30)
        datePicker.getComponent(1).preferredSize = Dimension(30, 30) //JButton

        datePanel.add(dateLabel)
        datePanel.add(datePicker)

        return datePanel
    }

    private fun createCommentPanel(): JPanel {
        commentTextField = JBTextField("")
        commentTextField.preferredSize = Dimension(389, typeComboBox.preferredSize.height)
        commentLabel.preferredSize = Dimension(labelsMargin, 30)

        val commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        return commentPanel
    }

    private fun createTimePanel(): JPanel {
        val timePanel = JPanel(FlowLayout(2))

        val hoursModel: SpinnerModel = SpinnerNumberModel(2,  //initial value
                0,  //min
                100,  //max
                1)
        hoursSpinner = JSpinner(hoursModel)
        hoursSpinner.editor = JSpinner.NumberEditor(hoursSpinner, "00")


        val minutesModel: SpinnerModel = SpinnerNumberModel(0,  //initial value
                0,  //min
                60,  //max
                1)
        minutesSpinner = JSpinner(minutesModel)
        minutesSpinner.editor = JSpinner.NumberEditor(minutesSpinner, "00")

        val inputTimePanel = JPanel(FlowLayout(3))
        inputTimePanel.add(hoursSpinner)
        inputTimePanel.add(JLabel(" hours "))
        inputTimePanel.add(minutesSpinner)
        inputTimePanel.add(JLabel(" minutes"))

        timeLabel.preferredSize = Dimension(labelsMargin - 5, 30)

        timePanel.add(timeLabel)
        timePanel.add(inputTimePanel)

        return timePanel
    }

    override fun createActions(): Array<out Action> = arrayOf()

    private fun createButtonsPanel(): JPanel {
        val buttonsPanel = JPanel(FlowLayout(2))
        okButton.addActionListener { okAction() }
        okButton.preferredSize = Dimension(90, 31)

        cancelButton.addActionListener { doCancelAction() }
        cancelButton.preferredSize = Dimension(90, 31)

        val sep = JLabel("")
        sep.preferredSize = Dimension((labelsMargin * 2.7).toInt(), 30)
        buttonsPanel.add(sep)
        buttonsPanel.add(cancelButton)
        buttonsPanel.add(okButton)

        return buttonsPanel
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(GridLayout())
        val mainPane = prepareMainPane()
        contextPane.apply {
            preferredSize = Dimension(530, 340)
            minimumSize = preferredSize
            add(mainPane)
        }
        return contextPane
    }

    private fun okAction() {
        val hours = hoursSpinner.value.toString()
        val minutes = minutesSpinner.value.toString()
        val time = TimeUnit.HOURS.toMinutes(hours.toLong()) + minutes.toLong()

        if (datePicker.date == null) {
            notifier.foreground = Color.red
            notifier.text = "Date is not specified"
        } else {
            try {
                val selectedIssueIndex = idComboBox.selectedIndex
                val future = TimeTrackingConfigurator().checkIfTrackingIsEnabledForIssue(repo, selectedIssueIndex, ids)

                if (future.get() == false) {
                    notifier.foreground = Color.red
                    notifier.text = "Time tracking is disabled for this project"
                    logger.debug("Time tracking for ${ids[idComboBox.selectedIndex].issueId} is disabled in project")
                } else {
                    val selectedId = ids[idComboBox.selectedIndex].issueId

                    val futureCode = TimeTrackerConnector(repo, project).addWorkItemManually(datePicker.date.format(),
                        typeComboBox.getItemAt(typeComboBox.selectedIndex), selectedId, commentTextField.text,
                        time.toString(), notifier)

                    if (futureCode.get() == 200) { this@TimeTrackerManualEntryDialog.close(0) }
                }
            } catch (e: IndexOutOfBoundsException) {
                notifier.foreground = Color.red
                notifier.text = "Please select the issue"
                logger.debug("Issue is not selected or there are no issues in the list: ${e.message}")
            }
        }
    }

}
