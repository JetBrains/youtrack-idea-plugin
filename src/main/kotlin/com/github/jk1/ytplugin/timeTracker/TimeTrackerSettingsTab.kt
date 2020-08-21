package com.github.jk1.ytplugin.timeTracker

import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel


class TimeTrackerSettingsTab() : JBPanel<JBPanelWithEmptyText>() {

    private var scheduledHour: JBTextField
    private var scheduledMinutes: JBTextField
    private var scheduledTextField = JBLabel("Scheduled posting at ")
    private var isScheduledCheckbox: JBCheckBox

    private var isManualModeCheckbox: JBCheckBox
    private var manualModeTextField = JBLabel("Enable manual mode")

    private var postWhenProjectClosedCheckbox: JBCheckBox
    private var postWhenProjectClosedTextField = JBLabel("Post time when project is closed")

    private var postWhenCommitCheckbox: JBCheckBox
    private var mpostWhenCommitTextField = JBLabel("Post time after commits")


    init{
        val scheduledPanel = JPanel(FlowLayout(2))
        val timePanel = JPanel(FlowLayout(3))
        scheduledHour = JBTextField("")
        scheduledMinutes = JBTextField("")

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

        scheduledPanel.add(scheduledTextField)
        scheduledPanel.add(timePanel)

        bigScheduledPanel.add(isScheduledCheckbox)
        bigScheduledPanel.add(scheduledPanel)


        val parametersPanel = JPanel(VerticalLayout(3))
        val manualPanel = JPanel(FlowLayout(2))
        manualPanel.add(isManualModeCheckbox)
        manualPanel.add(manualModeTextField)
        parametersPanel.add(manualPanel)

        val postWhenProjectClosedPanel = JPanel(FlowLayout(2))
        postWhenProjectClosedPanel.add(postWhenProjectClosedCheckbox)
        postWhenProjectClosedPanel.add(postWhenProjectClosedTextField)
        parametersPanel.add(postWhenProjectClosedPanel)

        val postWhenCommitPanel = JPanel(FlowLayout(2))
        postWhenCommitPanel.add(postWhenCommitCheckbox)
        postWhenCommitPanel.add(mpostWhenCommitTextField)
        parametersPanel.add(postWhenCommitPanel)

        layout = VerticalLayout(2)
        add(bigScheduledPanel)
        add(parametersPanel)
    }

}