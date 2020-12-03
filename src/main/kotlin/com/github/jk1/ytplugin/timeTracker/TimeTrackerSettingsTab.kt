package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.swing.ButtonGroup
import javax.swing.JComboBox
import javax.swing.JPanel


class TimeTrackerSettingsTab(val repo: YouTrackServer, val myHeight: Int, val myWidth: Int) : JBPanel<JBPanelWithEmptyText>() {

    private lateinit var scheduledHour: JBTextField
    private lateinit var scheduledMinutes: JBTextField

    private lateinit var inactivityHourInputField: JBTextField
    private lateinit var inactivityMinutesInputField: JBTextField
    private var inactivityTextField = JBLabel(" Inactivity period (hh/mm):")

    private lateinit var isScheduledCheckbox: JBCheckBox
    private var scheduledTextField = JBLabel("Scheduled posting at (hh/mm):")

    private lateinit var isManualModeRadioButton: JBRadioButton
    private var manualModeTextField = JBLabel("Enable manual mode           ")

    lateinit var noTrackingButton: JBRadioButton

    private lateinit var postWhenProjectClosedCheckbox: JBCheckBox
    private lateinit var postWhenCommitCheckbox: JBCheckBox

    private var postWhenProjectClosedTextField = JBLabel("Post time when project is closed")
    private var mpostWhenCommitTextField = JBLabel("Post time after commits")

    private lateinit var isAutoTrackingEnabledRadioButton: JBRadioButton
    private var autoTrackingEnabledTextField = JBLabel("Enable automated mode        ")

    private var commentLabel = JBLabel(" Comment:")
    private var typeLabel = JBLabel(" Work Item type:")
    private lateinit var commentTextField: JBTextField

    private val workItemsTypes = arrayOf<String?>("Development")

    private var typeComboBox = JComboBox(workItemsTypes)

    init {
        val timer = ComponentAware.of(repo.project).timeTrackerComponent

        val inactivityPeriodPanel = createInactivityPeriodPanel(timer)

        val trackingModePanel = createTrackingModePanel(timer, myHeight, myWidth)

        val schedulePanel = createScheduledPanel(timer)

        val typePanel = createTypePanel(timer, repo)

        val postWhenPanel = createPostWhenPanel(timer, myWidth, myHeight)

        val commentPanel = createCommentPanel(timer, myHeight, myWidth)

        layout = VerticalLayout(8)
        maximumSize = Dimension(myWidth, myHeight)
        minimumSize = Dimension(myWidth, myHeight)
        add(trackingModePanel)
        add(JBLabel(""))
        add(postWhenPanel)
        add(schedulePanel)
        add(inactivityPeriodPanel)
        add(typePanel)
        add(commentPanel)
    }

    private fun createCommentPanel(timer: TimeTracker, height: Int, width: Int): JPanel {

        commentTextField = JBTextField(timer.comment)
        commentTextField.preferredSize = Dimension((0.7 * width).toInt(), (0.0875 * height).toInt())
        val commentPanel = JPanel(FlowLayout(2))
        commentLabel.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        commentTextField.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable

        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.065 * myWidth).toInt() - 1, (0.0875 * height).toInt())

        commentPanel.add(commentLabel)
        commentPanel.add(sep)
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


        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.1 * width).toInt(), (0.0875 * height).toInt())
        postWhenPanel.add(sep)

        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(mpostWhenCommitTextField)

        postWhenProjectClosedCheckbox.isEnabled = timer.isAutoTrackingEnable
        postWhenProjectClosedTextField.isEnabled = timer.isAutoTrackingEnable
        postWhenCommitCheckbox.isEnabled = timer.isAutoTrackingEnable
        mpostWhenCommitTextField.isEnabled = timer.isAutoTrackingEnable

        return postWhenPanel
    }

    private fun createTrackingModePanel(timer: TimeTracker, height: Int, width: Int): JPanel {

        isAutoTrackingEnabledRadioButton = JBRadioButton()
        isAutoTrackingEnabledRadioButton.isSelected = timer.isAutoTrackingEnable
        isAutoTrackingEnabledRadioButton.addActionListener {
            isTrackingModeChanged(isAutoTrackingEnabledRadioButton.isSelected,
                    isManualModeRadioButton.isSelected, noTrackingButton.isSelected)
        }

        isManualModeRadioButton = JBRadioButton()
        isManualModeRadioButton.isSelected = timer.isManualTrackingEnable
        isManualModeRadioButton.addActionListener {
            isTrackingModeChanged(isAutoTrackingEnabledRadioButton.isSelected,
                    isManualModeRadioButton.isSelected, noTrackingButton.isSelected)
        }

        noTrackingButton = JBRadioButton("None")
        if (!repo.getRepo().isConfigured) {
            forbidSelection()
        } else {
            allowSelection()
        }
        noTrackingButton.addActionListener {
            isTrackingModeChanged(isAutoTrackingEnabledRadioButton.isSelected,
                    isManualModeRadioButton.isSelected, noTrackingButton.isSelected)
        }
        noTrackingButton.isSelected = true

        val buttonGroup = ButtonGroup()
        buttonGroup.add(isAutoTrackingEnabledRadioButton)
        buttonGroup.add(isManualModeRadioButton)
        buttonGroup.add(noTrackingButton)

        val enableAutoTrackingPanel = JPanel(FlowLayout(2))
        val enableManualTrackingPanel = JPanel(FlowLayout(2))
        enableAutoTrackingPanel.add(isAutoTrackingEnabledRadioButton)
        enableAutoTrackingPanel.add(autoTrackingEnabledTextField)
        enableManualTrackingPanel.add(isManualModeRadioButton)
        enableManualTrackingPanel.add(manualModeTextField)

        val trackingModePanel = JPanel(BorderLayout())

        trackingModePanel.add(enableAutoTrackingPanel, BorderLayout.WEST)
        trackingModePanel.add(enableManualTrackingPanel, BorderLayout.CENTER)
        trackingModePanel.add(noTrackingButton, BorderLayout.EAST)

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

        val inactivityPeriodPanel = JPanel(FlowLayout(3))

        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.1 * myWidth).toInt() - 2, (0.0875 * height).toInt())
        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(sep)
        inactivityPeriodPanel.add(inactivityTimePanel)

        return inactivityPeriodPanel
    }


    private fun isTrackingModeChanged(autoTrackEnabled: Boolean, manualTrackEnabled: Boolean, noTrackingEnabled: Boolean) {

        scheduledHour.isEnabled = autoTrackEnabled
        scheduledMinutes.isEnabled = autoTrackEnabled

        inactivityHourInputField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        inactivityMinutesInputField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        inactivityTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled

        scheduledTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        isScheduledCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled

        postWhenProjectClosedCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        postWhenProjectClosedTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled

        postWhenCommitCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        mpostWhenCommitTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled

        commentLabel.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        commentTextField.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        typeLabel.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        typeComboBox.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled

    }
    
    fun forbidSelection(){
        noTrackingButton.isEnabled = false
        isAutoTrackingEnabledRadioButton.isEnabled = false
        isManualModeRadioButton.isEnabled = false
        noTrackingButton.isSelected = true
        isTrackingModeChanged(autoTrackEnabled = false, manualTrackEnabled = false, noTrackingEnabled = false)
    }

    fun allowSelection(){
        noTrackingButton.isEnabled = true
        isAutoTrackingEnabledRadioButton.isEnabled = true
        isManualModeRadioButton.isEnabled = true
    }

    fun getAutoTrackingEnabledCheckBox() = isAutoTrackingEnabledRadioButton
    fun getType() = typeComboBox.getItemAt(typeComboBox.selectedIndex)
    fun getInactivityHours(): String = inactivityHourInputField.text
    fun getInactivityMinutes(): String = inactivityMinutesInputField.text
    fun getManualModeCheckbox() = isManualModeRadioButton
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