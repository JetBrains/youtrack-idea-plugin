package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.setup.SetupDialog
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel


class TimeTrackerSettingsTab(val repo: YouTrackServer, myHeight: Int, myWidth: Int) : JBPanel<JBPanelWithEmptyText>() {

    private lateinit var scheduledHour: JBTextField
    private lateinit var scheduledMinutes: JBTextField

    private lateinit var inactivityHourInputField: JBTextField
    private lateinit var inactivityMinutesInputField: JBTextField
    private var inactivityTextField = JBLabel(" Inactivity period (hh/mm): ")

    private lateinit var isScheduledCheckbox: JBCheckBox
    private var scheduledTextField = JBLabel("Scheduled posting at (hh/mm)")

    private lateinit var isManualModeCheckbox: JBCheckBox
    private var manualModeTextField = JBLabel("Enable manual mode          ")

    private lateinit var postWhenProjectClosedCheckbox: JBCheckBox
    private lateinit var postWhenCommitCheckbox: JBCheckBox

    private var postWhenProjectClosedTextField = JBLabel("Post time when project is closed")
    private var mpostWhenCommitTextField = JBLabel("Post time after commits")

    private lateinit var isAutoTrackingEnabledCheckBox: JBCheckBox
    private var autoTrackingEnabledTextField = JBLabel("Enable automated mode")

    private var commentLabel = JBLabel(" Comment:")
    private var typeLabel = JBLabel(" Spent time type:")
    private lateinit var commentTextField: JBTextField

    private val workItemsTypes = arrayOf<String?>("Development")

    private var typeComboBox = JComboBox(workItemsTypes)
    val okButton = JButton("OK")

    init {
        val timer = ComponentAware.of(repo.project).timeTrackerComponent

        val inactivityPeriodPanel = createInactivityPeriodPanel(timer)

        val trackingModePanel = createTrackingModePanel(timer, myHeight, myWidth)

        val schedulePanel = createScheduledPanel(timer)

        val typePanel = createTypePanel(timer, repo)

        val postWhenPanel = createPostWhenPanel(timer, myWidth, myHeight)

        val commentPanel = createCommentPanel(timer, myHeight, myWidth)

        layout = VerticalLayout(7)
        add(trackingModePanel)
        add(postWhenPanel)
        add(schedulePanel)
        add(inactivityPeriodPanel)
        add(typePanel)
        add(commentPanel)

    }

    private fun createCommentPanel(timer: TimeTracker, height: Int, width: Int): JPanel {

        commentTextField = JBTextField(timer.comment)
        commentTextField.preferredSize = Dimension((0.8 * width).toInt(), (0.0875 * height).toInt())
        val commentPanel = JPanel(FlowLayout(2))
        commentLabel.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        commentTextField.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        return commentPanel
    }

    private fun createTypePanel(timer: TimeTracker, repo: YouTrackServer): JPanel {

        val typePanel = JPanel(FlowLayout(2))
        val timerService = TimeTrackingService()
        val types = timerService.getAvailableWorkItemsTypes(repo)

        var idx = 0
        if (types.isNotEmpty()) {
            typeComboBox = JComboBox(types.toTypedArray())
            types.mapIndexed { index, value ->
                if (value == timer.type) {
                    idx = index
                }
            }
        }

        typeComboBox.selectedIndex = idx
        typeComboBox.isEditable = true
        typeComboBox.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        typeLabel.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable


        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)

        return typePanel
    }

    private fun createPostWhenPanel(timer: TimeTracker, width: Int, height: Int): JPanel {

        val postWhenPanel = JPanel(FlowLayout(5))

        postWhenCommitCheckbox = JBCheckBox()
        postWhenCommitCheckbox.isSelected = timer.isPostAfterCommitEnabled

        postWhenProjectClosedCheckbox = JBCheckBox()
        postWhenProjectClosedCheckbox.isSelected = timer.isWhenProjectClosedEnabled
        postWhenPanel.add(postWhenProjectClosedCheckbox)
        postWhenPanel.add(postWhenProjectClosedTextField)


        val separator2 = JBLabel("")
        separator2.preferredSize = Dimension((0.1 * width).toInt(), (0.0875 * height).toInt())
        postWhenPanel.add(separator2)

        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(mpostWhenCommitTextField)

        postWhenProjectClosedCheckbox.isEnabled = timer.isAutoTrackingEnable
        postWhenProjectClosedTextField.isEnabled = timer.isAutoTrackingEnable
        postWhenCommitCheckbox.isEnabled = timer.isAutoTrackingEnable
        mpostWhenCommitTextField.isEnabled = timer.isAutoTrackingEnable

        return postWhenPanel
    }

    private fun createTrackingModePanel(timer: TimeTracker, height: Int, width: Int): JPanel {

        isAutoTrackingEnabledCheckBox = JBCheckBox()
        isAutoTrackingEnabledCheckBox.isSelected = timer.isAutoTrackingEnable
        isAutoTrackingEnabledCheckBox.addActionListener {
            isAutoTrackingChanged(isAutoTrackingEnabledCheckBox.isSelected,
                    isManualModeCheckbox.isSelected)
        }

        isManualModeCheckbox = JBCheckBox()
        isManualModeCheckbox.isSelected = timer.isManualTrackingEnable
        isManualModeCheckbox.addActionListener {
            isAutoTrackingChanged(isAutoTrackingEnabledCheckBox.isSelected,
                    isManualModeCheckbox.isSelected)
        }

        val enableAutoTrackingPanel = JPanel(FlowLayout(2))
        val enableManualTrackingPanel = JPanel(FlowLayout(2))
        enableAutoTrackingPanel.add(isAutoTrackingEnabledCheckBox)
        enableAutoTrackingPanel.add(autoTrackingEnabledTextField)
        enableManualTrackingPanel.add(isManualModeCheckbox)
        enableManualTrackingPanel.add(manualModeTextField)

        val trackingModePanel = JPanel(BorderLayout())
        val separator1 = JBLabel("")
        separator1.preferredSize = Dimension((0.195 * width).toInt(), (0.0875 * height).toInt())
        trackingModePanel.add(enableAutoTrackingPanel, BorderLayout.WEST)
        trackingModePanel.add(separator1, BorderLayout.CENTER)
        trackingModePanel.add(enableManualTrackingPanel, BorderLayout.EAST)

        return trackingModePanel
    }


    private fun createScheduledPanel(timer: TimeTracker): JPanel {

        val timePanel = JPanel(FlowLayout(3))

        scheduledHour = JBTextField(timer.scheduledPeriod.substring(0, 2))
        scheduledMinutes = JBTextField(timer.scheduledPeriod.substring(3, 5))

        isScheduledCheckbox = JBCheckBox()
        isScheduledCheckbox.isSelected = timer.isScheduledEnabled

        timePanel.add(scheduledHour)
        timePanel.add(JBLabel(":"))
        timePanel.add(scheduledMinutes)

        val bigScheduledPanel = JPanel(FlowLayout(2))
        val scheduledPanel = JPanel(FlowLayout(2))

        scheduledTextField.isEnabled = timer.isAutoTrackingEnable
        isScheduledCheckbox.isEnabled = timer.isAutoTrackingEnable
        scheduledHour.isEnabled = timer.isAutoTrackingEnable
        scheduledMinutes.isEnabled = timer.isAutoTrackingEnable

        scheduledPanel.add(scheduledTextField)
        scheduledPanel.add(timePanel)

        bigScheduledPanel.add(isScheduledCheckbox)
        bigScheduledPanel.add(scheduledPanel)

        return bigScheduledPanel
    }

    private fun createInactivityPeriodPanel(timer: TimeTracker): JPanel {

        val inactivityTimePanel = JPanel(FlowLayout(3))

        val inactivityHours = TimeUnit.MILLISECONDS.toHours(timer.inactivityPeriodInMills)
        val inactivityMinutes = TimeUnit.MILLISECONDS.toMinutes(timer.inactivityPeriodInMills -
                TimeUnit.HOURS.toMillis(inactivityHours))

        inactivityHourInputField = JBTextField(inactivityHours.toString())
        inactivityMinutesInputField = JBTextField(inactivityMinutes.toString())

        inactivityTimePanel.add(inactivityHourInputField)
        inactivityTimePanel.add(JBLabel(":"))
        inactivityTimePanel.add(inactivityMinutesInputField)

        inactivityHourInputField.isEnabled = timer.isAutoTrackingEnable
        inactivityMinutesInputField.isEnabled = timer.isAutoTrackingEnable
        inactivityTextField.isEnabled = timer.isAutoTrackingEnable

        val inactivityPeriodPanel = JPanel(FlowLayout(2))

        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(inactivityTimePanel)

        return inactivityPeriodPanel
    }


    private fun isAutoTrackingChanged(autoTrackEnabled: Boolean, manualTrackEnabled: Boolean) {

        scheduledHour.isEnabled = autoTrackEnabled
        scheduledMinutes.isEnabled = autoTrackEnabled

        inactivityHourInputField.isEnabled = autoTrackEnabled
        inactivityMinutesInputField.isEnabled = autoTrackEnabled
        inactivityTextField.isEnabled = autoTrackEnabled

        scheduledTextField.isEnabled = autoTrackEnabled
        isScheduledCheckbox.isEnabled = autoTrackEnabled

        postWhenProjectClosedCheckbox.isEnabled = autoTrackEnabled
        postWhenProjectClosedTextField.isEnabled = autoTrackEnabled

        postWhenCommitCheckbox.isEnabled = autoTrackEnabled
        mpostWhenCommitTextField.isEnabled = autoTrackEnabled

        commentLabel.isEnabled = autoTrackEnabled || manualTrackEnabled
        commentTextField.isEnabled = autoTrackEnabled || manualTrackEnabled
        typeLabel.isEnabled = autoTrackEnabled || manualTrackEnabled
        typeComboBox.isEnabled = autoTrackEnabled || manualTrackEnabled

    }

    fun getAutoTrackingEnabledCheckBox() = isAutoTrackingEnabledCheckBox
    fun getType() = typeComboBox.getItemAt(typeComboBox.selectedIndex)
    fun getInactivityHours(): String = inactivityHourInputField.text
    fun getInactivityMinutes(): String = inactivityMinutesInputField.text
    fun getManualModeCheckbox() = isManualModeCheckbox
    fun getScheduledCheckbox() = isScheduledCheckbox
    fun getPostWhenCommitCheckbox() = postWhenCommitCheckbox

    fun getScheduledTime(): String {
        val formatter = SimpleDateFormat("mm")
        val hours = formatter.format(SimpleDateFormat("mm").parse(scheduledHour.text))
        return "$hours:${formatter.format(SimpleDateFormat("mm").parse(scheduledMinutes.text))}:0"
    }

    fun getComment(): String = commentTextField.text
    fun getPostOnClose() = postWhenProjectClosedCheckbox

}