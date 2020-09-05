package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JComboBox
import javax.swing.JPanel


class TimeTrackerSettingsTab(repo: YouTrackServer, height: Int, width: Int) : JBPanel<JBPanelWithEmptyText>() {

    private var scheduledHour: JBTextField
    private var scheduledMinutes: JBTextField

    private var isAutoTrackingEnabledCheckBox: JBCheckBox
    private var autoTrackingEnabledTextField = JBLabel("Enable automated mode      ")

    private var inactivityHourInputField: JBTextField
    private var inactivityMinutesInputField: JBTextField
    private var inactivityTextField = JBLabel("Inactivity period (hh/mm): ")

    private var scheduledTextField = JBLabel("Scheduled posting at (hh/mm)")
    private var isScheduledCheckbox: JBCheckBox

    private var isManualModeCheckbox: JBCheckBox
    private var manualModeTextField = JBLabel("Enable manual mode")

    private var postWhenProjectClosedCheckbox: JBCheckBox
    private var postWhenProjectClosedTextField = JBLabel("Post time when project is closed")

    private var postWhenCommitCheckbox: JBCheckBox
    private var mpostWhenCommitTextField = JBLabel("Post time after commits")

    private val postWhenPanel: JPanel
    private val typePanel: JPanel
    private val parametersPanel: JPanel

    private var commentLabel= JBLabel("Comment:")
    private var typeLabel = JBLabel("Spent time type:")
    private var commentTextField: JBTextField

    val workItemsTypes= arrayOf<String?>("Development")

    private var typeComboBox =  JComboBox(workItemsTypes)


    init{
        val timePanel = JPanel(FlowLayout(3))
        val inactivityTimePanel = JPanel(FlowLayout(3))

        isAutoTrackingEnabledCheckBox = JBCheckBox()
        isAutoTrackingEnabledCheckBox.isSelected = true
        isAutoTrackingEnabledCheckBox.addActionListener { isAutoTrackingChanged(isAutoTrackingEnabledCheckBox.isSelected) }


        scheduledHour = JBTextField("19")
        scheduledMinutes = JBTextField("00")

        inactivityHourInputField = JBTextField("00")
        inactivityMinutesInputField = JBTextField("10")

        isScheduledCheckbox = JBCheckBox()
        isScheduledCheckbox.isSelected = true

        isManualModeCheckbox = JBCheckBox()
        isManualModeCheckbox.isSelected = true

        postWhenCommitCheckbox = JBCheckBox()
        postWhenCommitCheckbox.isSelected = true

        postWhenProjectClosedCheckbox = JBCheckBox()
        postWhenProjectClosedCheckbox.isSelected = true

        timePanel.add(scheduledHour)
        timePanel.add(JBLabel(":"))
        timePanel.add(scheduledMinutes)

        val bigScheduledPanel = JPanel(FlowLayout(2))
        val scheduledPanel = JPanel(FlowLayout(2))

        scheduledPanel.add(scheduledTextField)
        scheduledPanel.add(timePanel)

        bigScheduledPanel.add(isScheduledCheckbox)
        bigScheduledPanel.add(scheduledPanel)

        inactivityTimePanel.add(inactivityHourInputField)
        inactivityTimePanel.add(JBLabel(":"))
        inactivityTimePanel.add(inactivityMinutesInputField)

        val inactivityPeriodPanel = JPanel(FlowLayout(2))

        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(inactivityTimePanel)

        val enableAutoTrackingPanel = JPanel(FlowLayout(2))
        val enableManualTrackingPanel = JPanel(FlowLayout(2))
        enableAutoTrackingPanel.add(isAutoTrackingEnabledCheckBox)
        enableAutoTrackingPanel.add(autoTrackingEnabledTextField)
        enableManualTrackingPanel.add(isManualModeCheckbox)
        enableManualTrackingPanel.add(manualModeTextField)

        val bigTrackingPanel = JPanel(FlowLayout(3))
        val separator1 = JBLabel("")
        separator1.preferredSize = Dimension((0.135 * width).toInt(), (0.0875 * height).toInt())
        bigTrackingPanel.add(enableAutoTrackingPanel)
        bigTrackingPanel.add(separator1)
        bigTrackingPanel.add(enableManualTrackingPanel)

        parametersPanel = JPanel(VerticalLayout(3))

        postWhenPanel = JPanel(FlowLayout(5))
        postWhenPanel.add(postWhenProjectClosedCheckbox)
        postWhenPanel.add(postWhenProjectClosedTextField)
        // TODO: remove hardcode
        val separator2 = JBLabel("")
        separator2.preferredSize = Dimension((0.1 * width).toInt(), (0.0875 * height).toInt())
        postWhenPanel.add(separator2)

        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(mpostWhenCommitTextField)

        parametersPanel.add( postWhenPanel)
        parametersPanel.add(bigScheduledPanel)

        commentTextField = JBTextField("")
        commentTextField.preferredSize = Dimension((0.8 * width).toInt(), (0.0875 * height).toInt())

        val types = mutableListOf<String>()
        TimeTrackerRestClient(repo).getAvailableWorkItemTypes().map { types.add(it.name) }

        if (types.isNotEmpty()){
            typeComboBox =  JComboBox(types.toTypedArray())
        }
        typeComboBox.selectedIndex = 0
        typeComboBox.isEditable = true

        typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        val commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        layout = VerticalLayout(4)
        add(bigTrackingPanel)
        add(parametersPanel)
        add(inactivityPeriodPanel)
        add(typePanel)
        add(commentPanel)
    }

    private fun isAutoTrackingChanged(enabled: Boolean) {

        typeComboBox.isEnabled = enabled
        typeLabel.isEnabled = enabled

        scheduledHour.isEnabled = enabled
        scheduledMinutes.isEnabled = enabled

        inactivityHourInputField.isEnabled = enabled
        inactivityMinutesInputField.isEnabled = enabled
        inactivityTextField.isEnabled = enabled

        scheduledTextField.isEnabled = enabled
        isScheduledCheckbox.isEnabled = enabled

        postWhenProjectClosedCheckbox.isEnabled = enabled
        postWhenProjectClosedTextField.isEnabled = enabled

        postWhenCommitCheckbox.isEnabled = enabled
        mpostWhenCommitTextField.isEnabled = enabled
    }

    fun getAutoTrackingEnabledCheckBox() = isAutoTrackingEnabledCheckBox
    fun getType() = typeComboBox.getItemAt(typeComboBox.selectedIndex)
    fun getInactivityHours(): String = inactivityHourInputField.text
    fun getInactivityMinutes(): String = inactivityMinutesInputField.text
    fun getManualModeCheckbox() = isManualModeCheckbox
    fun getScheduledCheckbox() = isScheduledCheckbox
    fun getScheduledHours(): String = scheduledHour.text
    fun getScheduledMinutes(): String = scheduledMinutes.text
    fun getComment(): String = commentTextField.text


}