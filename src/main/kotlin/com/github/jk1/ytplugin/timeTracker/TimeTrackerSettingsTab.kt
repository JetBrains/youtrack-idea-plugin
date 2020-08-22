package com.github.jk1.ytplugin.timeTracker

import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.FlowLayout
import java.awt.Rectangle
import javax.swing.JComboBox
import javax.swing.JPanel


class TimeTrackerSettingsTab() : JBPanel<JBPanelWithEmptyText>() {

    private var scheduledHour: JBTextField
    private var scheduledMinutes: JBTextField

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

    private var commentLabel= JBLabel("Comment:")
    private var typeLabel = JBLabel("Work items type:")
    private var commentTextField: JBTextField

    val workItemsTypes= arrayOf<String?>("Development", "Testing", "Documentation", "Other...")

    private var typeComboBox =  JComboBox(workItemsTypes)


    init{
        val timePanel = JPanel(FlowLayout(3))
        val inactivityTimePanel = JPanel(FlowLayout(3))

        scheduledHour = JBTextField("19")
        scheduledMinutes = JBTextField("00")

        inactivityHour = JBTextField("")
        inactivityMinutes = JBTextField("")

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

        val parametersPanel = JPanel(VerticalLayout(3))
        val manualPanel = JPanel(FlowLayout(4))
        manualPanel.add(bigScheduledPanel)
        manualPanel.add(JBLabel(""))
        manualPanel.add(isManualModeCheckbox)
        manualPanel.add(manualModeTextField)
        parametersPanel.add(manualPanel)

        val postWhenPanel = JPanel(FlowLayout(5))
        postWhenPanel.add(postWhenProjectClosedCheckbox)
        postWhenPanel.add(postWhenProjectClosedTextField)
        // TODO: remove hardcore
        postWhenPanel.add(JBLabel(""))

        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(mpostWhenCommitTextField)
        parametersPanel.add( postWhenPanel)

        commentTextField = JBTextField("")
        typeComboBox.selectedIndex = 0
        typeComboBox.isEditable = true

        val typePanel = JPanel(FlowLayout(2))
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        val commentPanel = JPanel(FlowLayout(2))
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        layout = VerticalLayout(4)
        add(parametersPanel)
        add(inactivityPeriodPanel)
        add(typePanel)
        add(commentPanel)

    }
}