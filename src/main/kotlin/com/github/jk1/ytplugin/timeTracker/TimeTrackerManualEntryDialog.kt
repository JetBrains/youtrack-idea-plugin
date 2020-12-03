package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssuesRestClient
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.*


open class TimeTrackerManualEntryDialog(override val project: Project, val repo: YouTrackServer) : DialogWrapper(project, false), ComponentAware {

    // TODO is hardcode removable here (for the sake of better look)
    private var dateLabel = JBLabel("Date:")
    private val datePicker = JXDatePicker(Date())
    private var idLabel = JBLabel("Issue:   ")

    // TODO: another comboBoxes
    private var idComboBox = JComboBox(arrayOf<String>())
    private var typeComboBox = JComboBox(arrayOf("Development"))
    private var timeLabel = JBLabel("Spent time:")

    private lateinit var hoursSpinner: JSpinner
    private lateinit var minutesSpinner: JSpinner

    private val okButton = JButton("Save")
    private val cancelButton = JButton("Cancel")

    private var commentLabel = JBLabel("Comment:")
    private var typeLabel = JBLabel("Work type:")
    private lateinit var commentTextField: JBTextField

    private var notifier = JBLabel("")

    private val ids = IssuesRestClient(repo).getFormattedUniqueIssueIds()
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

        val timerService = TimeTrackingService()
        val types = timerService.getAvailableWorkItemsTypes(repo)

        typeComboBox = JComboBox(types.toTypedArray())
        try {
            typeComboBox.selectedIndex = 0
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
            tasksIdRepresentation.add(id.value)
            tasksIds.add(id.name)
        }
        idComboBox = JComboBox(tasksIdRepresentation.toTypedArray())

        idComboBox.preferredSize = Dimension(390, typeComboBox.preferredSize.height)

        idComboBox.selectedIndex = tasksIds.indexOf(TaskManager.getManager(project).activeTask.id)

        idLabel.preferredSize = Dimension(labelsMargin, 30)
        idPanel.add(idLabel)
        idPanel.add(idComboBox)

        return idPanel
    }

    private fun createDatePanel(): JPanel {
        val datePanel = JPanel(FlowLayout(2))
        dateLabel.preferredSize = Dimension(labelsMargin + 3, 30)
        datePicker.preferredSize = Dimension(200, 25)
        datePicker.getComponent(1).preferredSize = Dimension(30, 30) //JButton

        datePanel.add(dateLabel)
        datePanel.add(datePicker)

        return datePanel
    }

    private fun createCommentPanel(): JPanel {
        commentTextField = JBTextField("")
        commentTextField.preferredSize = Dimension(390, typeComboBox.preferredSize.height)
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
        inputTimePanel.add(JLabel(":"))
        inputTimePanel.add(minutesSpinner)
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
            val selectedId = ids[idComboBox.selectedIndex].name
            val timerService = TimeTrackingService()
            val codeOnPost = timerService.postNewWorkItem(datePicker.date.format(),
                    typeComboBox.getItemAt(typeComboBox.selectedIndex), selectedId, repo,
                    commentTextField.text, time.toString())
            if (codeOnPost == 200) {
                this@TimeTrackerManualEntryDialog.close(0)
            }
            else {
                notifier.foreground = Color.red
                notifier.text = "Time could not be posted, code $codeOnPost"
            }
        }
    }

}

