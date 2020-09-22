package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.rest.IssuesRestClient
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

    // TODO is hardcode removable here (for the sake of better look)
    private var dateLabel = JBLabel("                         Date:")
    private val datePicker = JXDatePicker()
    private var idLabel = JBLabel("                       Issue:")
    // TODO: another comboBoxes
    private var idComboBox = JComboBox(arrayOf<String>())
    private var typeComboBox =  JComboBox(arrayOf<String>("Development"))
    private var timeLabel = JBLabel("     Time (hh/mm):")

    private lateinit var commentPanel: JPanel
    private lateinit var typePanel: JPanel
    private lateinit var hoursSpinner: JSpinner
    private lateinit var minutesSpinner: JSpinner

    private var commentLabel= JBLabel("              Comment:")
    private var typeLabel = JBLabel("   Work item type:")
    private lateinit var commentTextField: JBTextField

    private var notifier = JBLabel("" )

    private val ids = IssuesRestClient(repo).getFormattedUniqueIssueIds()
    private val tasksIdRepresentation = mutableListOf<String>()
    private val tasksIds  = mutableListOf<String>()

    init {
        title = "Add spent time"
    }

    override fun show() {
        init()
        super.show()
    }

    private fun prepareMainPane() : JPanel{

        val picker = JXDatePicker()
        picker.isEditable = true
        picker.date = Calendar.getInstance().time
        picker.setFormats(SimpleDateFormat("dd.MM.yyyy"))

        val idPanel = JPanel(FlowLayout(2))
        for (id in ids){
            tasksIdRepresentation.add(id.value)
            tasksIds.add(id.name)
        }
        idComboBox =  JComboBox(tasksIdRepresentation.toTypedArray())
        idComboBox.selectedIndex = tasksIds.indexOf( TaskManager.getManager(project).activeTask.id)

        idPanel.add(idLabel)
        idPanel.add(idComboBox)

        val timePanel = JPanel(FlowLayout(2))
        val datePanel = JPanel(FlowLayout(2))

        datePanel.add(dateLabel)
        datePanel.add(datePicker)

        val hoursModel: SpinnerModel = SpinnerNumberModel(2,  //initial value
                0,  //min
                100,  //max
                1)
        hoursSpinner = JSpinner(hoursModel)

        val minutesModel: SpinnerModel = SpinnerNumberModel(30,  //initial value
                0,  //min
                60,  //max
                1)
        minutesSpinner = JSpinner(minutesModel)

        commentTextField = JBTextField("")
        commentTextField.preferredSize = Dimension(390, 28)


        val inputTimePanel = JPanel(FlowLayout(3))
        inputTimePanel.add(hoursSpinner)
        inputTimePanel.add(JLabel(":"))
        inputTimePanel.add(minutesSpinner)

        timePanel.add(timeLabel)
        timePanel.add(inputTimePanel)

        commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        val types = mutableListOf<String>()
        TimeTrackerRestClient(repo).getAvailableWorkItemTypes().map { types.add(it.name) }

        typeComboBox =  JComboBox(types.toTypedArray())
        typeComboBox.selectedIndex = 0
        typeComboBox.isEditable = true

        typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        return JPanel().apply {
            layout = VerticalLayout(4)
            add(timePanel)
            add(idPanel)
            add(typePanel)
            add(commentPanel)
            add(datePanel)
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
            preferredSize = Dimension(530, 250)
            minimumSize = preferredSize
            add(mainPane)
        }
        return contextPane
    }


    inner class OkAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            var status = 0
            val date: Date

            val hours = hoursSpinner.value.toString()
            val minutes = minutesSpinner.value.toString()
            val time = hours.toInt() * 60 + minutes.toInt()

            if (datePicker.date == null) {
                notifier.foreground = Color.red
                notifier.text = " Date is not specified"
            } else {
                val selectedId = ids[idComboBox.selectedIndex].name
                val sdf = SimpleDateFormat("dd MMM yyyy")
                date = sdf.parse(datePicker.date.format())
                typeComboBox.getItemAt(typeComboBox.selectedIndex)?.let {
                    status = TimeTrackerRestClient(repo).postNewWorkItem(selectedId,
                            time.toString(), it, commentTextField.text, date.time.toString())
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

    fun getId() = ids[idComboBox.selectedIndex].name
    fun getTime() = hoursSpinner.value.toString() + ":" + minutesSpinner.value.toString()

}

