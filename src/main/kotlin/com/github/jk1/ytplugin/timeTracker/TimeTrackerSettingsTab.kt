package com.github.jk1.ytplugin.timeTracker

import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.FlowLayout
import javax.swing.JComboBox
import javax.swing.JPanel


class TimeTrackerSettingsTab() : JBPanel<JBPanelWithEmptyText>() {

    private var scheduledHour: JBTextField
    private var scheduledMinutes: JBTextField

    private var isAutoTrackingEnabledCheckBox: JBCheckBox
    private var autoTrackingEnabledTextField = JBLabel("Enable automated time tracking")

    private var inactivityHour: JBTextField
    private var inactivityMinutes: JBTextField
    private var inactivityTextField = JBLabel("Inactivity period: ")

    private var scheduledTextField = JBLabel("Scheduled posting at ")
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
    private val manualPanel: JPanel

    private var commentLabel= JBLabel("Comment:")
    private var typeLabel = JBLabel("Work items type:")
    private var commentTextField: JBTextField

    val workItemsTypes= arrayOf<String?>("Development", "Testing", "Documentation", "None", "Other...")

    private var typeComboBox =  JComboBox(workItemsTypes)


    init{
        val timePanel = JPanel(FlowLayout(3))
        val inactivityTimePanel = JPanel(FlowLayout(3))

        isAutoTrackingEnabledCheckBox = JBCheckBox()
        isAutoTrackingEnabledCheckBox.isSelected = true
        isAutoTrackingEnabledCheckBox.addActionListener { isAutoTrackingChanged(isAutoTrackingEnabledCheckBox.isSelected) }


        scheduledHour = JBTextField("19")
        scheduledMinutes = JBTextField("00")

        inactivityHour = JBTextField("00")
        inactivityMinutes = JBTextField("10")

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

        inactivityTimePanel.add(inactivityHour)
        inactivityTimePanel.add(JBLabel(":"))
        inactivityTimePanel.add(inactivityMinutes)

        val inactivityPeriodPanel = JPanel(FlowLayout(2))

        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(inactivityTimePanel)

        val enableAutoTrackingPanel = JPanel(FlowLayout(2))
        enableAutoTrackingPanel.add(isAutoTrackingEnabledCheckBox)
        enableAutoTrackingPanel.add(autoTrackingEnabledTextField)


        parametersPanel = JPanel(VerticalLayout(3))
        manualPanel = JPanel(FlowLayout(4))
        manualPanel.add(bigScheduledPanel)
        manualPanel.add(JBLabel(""))
        manualPanel.add(isManualModeCheckbox)
        manualPanel.add(manualModeTextField)
        parametersPanel.add(manualPanel)

        postWhenPanel = JPanel(FlowLayout(5))
        postWhenPanel.add(postWhenProjectClosedCheckbox)
        postWhenPanel.add(postWhenProjectClosedTextField)
        // TODO: remove hardcode
        postWhenPanel.add(JBLabel(""))

        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(mpostWhenCommitTextField)
        parametersPanel.add( postWhenPanel)

        commentTextField = JBTextField("")
        typeComboBox.selectedIndex = 3
        typeComboBox.isEditable = true

        typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        val commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        layout = VerticalLayout(4)
        add(enableAutoTrackingPanel)
        add(parametersPanel)
        add(inactivityPeriodPanel)
        add(typePanel)
//        add(commentPanel)
    }

    private fun isAutoTrackingChanged(enabled: Boolean) {

        typeComboBox.isEnabled = enabled
        typeLabel.isEnabled = enabled

        scheduledHour.isEnabled = enabled
        scheduledMinutes.isEnabled = enabled

        inactivityHour.isEnabled = enabled
        inactivityMinutes.isEnabled = enabled
        inactivityTextField.isEnabled = enabled

        scheduledTextField.isEnabled = enabled
        isScheduledCheckbox.isEnabled = enabled

        isManualModeCheckbox.isEnabled = enabled
        manualModeTextField.isEnabled = enabled

        postWhenProjectClosedCheckbox.isEnabled = enabled
        postWhenProjectClosedTextField.isEnabled = enabled

        postWhenCommitCheckbox.isEnabled = enabled
        mpostWhenCommitTextField.isEnabled = enabled
    }

    fun getAutoTrackingEnabledCheckBox() = isAutoTrackingEnabledCheckBox

}