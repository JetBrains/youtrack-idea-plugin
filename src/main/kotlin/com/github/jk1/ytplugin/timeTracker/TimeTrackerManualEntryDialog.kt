package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import org.jdesktop.swingx.JXDatePicker
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*


open class TimeTrackerManualEntryDialog(override val project: Project, val repo: YouTrackServer) : DialogWrapper(project, false), ComponentAware {


    private var dateLabel = JBLabel("Date:")
    private val datePicker = JXDatePicker()

    private var timeLabel = JBLabel("Time:")
    private lateinit var timeTextField: JBTextField

    private lateinit var commentPanel: JPanel

    private lateinit var typePanel: JPanel
    private lateinit var parametersPanel: JPanel

    private var commentLabel= JBLabel("Comment:")
    private var typeLabel = JBLabel("Work item type:")
    private lateinit var commentTextField: JBTextField

    val workItemsTypes= arrayOf<String?>("Development", "Testing", "Documentation", "None", "Other...")

    private var typeComboBox =  JComboBox(workItemsTypes)

    init {
        title = "Post work item manually"
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
        commentTextField.preferredSize = Dimension(345, 34)

        // displayPanel.add( titleText, BorderLayout.NORTH );
        timePanel.add(timeLabel)
        timePanel.add(timeTextField)

        parametersPanel = JPanel(FlowLayout(2))
        parametersPanel.add(datePanel)

        parametersPanel.add(timePanel)

        commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        typeComboBox.selectedIndex = 3
        typeComboBox.isEditable = true

        typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)


        return JPanel().apply {
            layout = VerticalLayout(3)
            add(parametersPanel)
            add(typePanel)
            add(commentPanel)
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
        val tabbedPane = prepareMainPane()
        contextPane.apply {
            preferredSize = Dimension(440, 150)
            minimumSize = preferredSize
            add(tabbedPane)
        }
        return contextPane
    }

    inner class OkAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
        }
    }

}

