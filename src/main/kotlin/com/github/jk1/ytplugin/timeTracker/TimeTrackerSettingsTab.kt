package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.ui.components.*
import com.jetbrains.rd.swing.textProperty
import java.awt.Dimension
import java.awt.FlowLayout
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.border.EtchedBorder
import javax.swing.border.TitledBorder


class TimeTrackerSettingsTab(val repo: YouTrackServer, val myHeight: Int, val myWidth: Int) : JBPanel<JBPanelWithEmptyText>() {

    private lateinit var scheduledHour: JBTextField
    private lateinit var scheduledMinutes: JBTextField

    private lateinit var inactivityHourInputField: JBTextField
    private lateinit var inactivityMinutesInputField: JBTextField
    private var inactivityTextField = JBLabel(" Inactivity period:")

    private lateinit var isScheduledCheckbox: JBCheckBox
    private var scheduledTextField = JBLabel("On a set schedule at:")

    private lateinit var isManualModeRadioButton: JBRadioButton
    private var manualModeTextField = JBLabel("Manual")

    lateinit var noTrackingButton: JBRadioButton

    private lateinit var postWhenProjectClosedCheckbox: JBCheckBox
    private lateinit var postWhenCommitCheckbox: JBCheckBox

    private var postWhenProjectClosedTextField = JBLabel("When closing the project")
    private var postWhenCommitTextField = JBLabel("When committing changes  ")

    private lateinit var isAutoTrackingEnabledRadioButton: JBRadioButton
    private var autoTrackingEnabledTextField = JBLabel("Automatic")

    private var commentLabel = JBLabel(" Comment: ")
    private var typeLabel = JBLabel(" Work type:")
    private lateinit var commentTextField: JBTextField

    private var hourLabel1 =  JBLabel(" hours ")
    private var hourLabel2 =  JBLabel(" hours ")
    private var minuteLabel1 =  JBLabel(" minutes")
    private var minuteLabel2 =  JBLabel(" minutes")

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

        val autoPanel = JPanel(VerticalLayout(7))
        val loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
        val autoPanelBorder = BorderFactory.createTitledBorder(loweredetched, "Automatically create work items")
        autoPanelBorder.titlePosition = TitledBorder.TOP
        autoPanel.border = autoPanelBorder
        autoPanel.add(postWhenPanel)
        autoPanel.add(schedulePanel)

        val preferencesPanel = JPanel(VerticalLayout(7))
        val preferencesPanelBorder = BorderFactory.createTitledBorder(loweredetched, "Preferences")
        preferencesPanelBorder.titlePosition = TitledBorder.TOP
        preferencesPanel.border = preferencesPanelBorder
        preferencesPanel.add(inactivityPeriodPanel)
        preferencesPanel.add(typePanel)
        preferencesPanel.add(commentPanel)


        add(trackingModePanel)
        add(JBLabel(""))
        add(autoPanel)
        add(preferencesPanel)
    }

    private fun createCommentPanel(timer: TimeTracker, height: Int, width: Int): JPanel {

        commentTextField = JBTextField(timer.comment)
        commentTextField.toolTipText = "Enter default comment text"
        commentTextField.preferredSize = Dimension(401, 30)
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

        val sep = JBLabel("")
        sep.preferredSize = Dimension(70, (0.0875 * height).toInt())

        postWhenPanel.add(sep)
        postWhenPanel.add(postWhenCommitCheckbox)
        postWhenPanel.add(postWhenCommitTextField)

        postWhenProjectClosedCheckbox.isEnabled = timer.isAutoTrackingEnable
        postWhenProjectClosedTextField.isEnabled = timer.isAutoTrackingEnable
        postWhenCommitCheckbox.isEnabled = timer.isAutoTrackingEnable
        postWhenCommitTextField.isEnabled = timer.isAutoTrackingEnable

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

        noTrackingButton = JBRadioButton("Off  ")
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

        val sep = JBLabel(" ")
        sep.preferredSize = Dimension(110, 30)
        val sep2 = JBLabel(" ")
        sep2.preferredSize = Dimension(110, 30)
        enableAutoTrackingPanel.add(isAutoTrackingEnabledRadioButton)
        enableAutoTrackingPanel.add(autoTrackingEnabledTextField)
        enableManualTrackingPanel.add(isManualModeRadioButton)
        enableManualTrackingPanel.add(manualModeTextField)

        val trackingModePanel = JPanel(FlowLayout(5))

        trackingModePanel.add(enableAutoTrackingPanel)
        trackingModePanel.add(sep)
        trackingModePanel.add(enableManualTrackingPanel)
        trackingModePanel.add(sep2)
        trackingModePanel.add(noTrackingButton)


        val loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
        val trackingModeBorder = BorderFactory.createTitledBorder(loweredetched, "Tracking mode")
        trackingModeBorder.titlePosition = TitledBorder.TOP
        trackingModePanel.border = trackingModeBorder

        return trackingModePanel
    }


    private fun createScheduledPanel(timer: TimeTracker): JPanel {

        val timePanel = JPanel(FlowLayout(3))

        scheduledHour = JBTextField(timer.scheduledPeriod.substring(0, 2))
        scheduledMinutes = JBTextField(timer.scheduledPeriod.substring(3, 5))

        isScheduledCheckbox = JBCheckBox()
        isScheduledCheckbox.isSelected = timer.isScheduledEnabled

        timePanel.add(scheduledHour)
        timePanel.add(hourLabel2)
        timePanel.add(scheduledMinutes)
        timePanel.add(minuteLabel2)

        val bigScheduledPanel = JPanel(FlowLayout(2))
        val scheduledPanel = JPanel(FlowLayout(2))

        scheduledTextField.isEnabled = timer.isAutoTrackingEnable
        isScheduledCheckbox.isEnabled = timer.isAutoTrackingEnable
        scheduledHour.isEnabled = timer.isAutoTrackingEnable
        scheduledMinutes.isEnabled = timer.isAutoTrackingEnable
        hourLabel2.isEnabled = timer.isAutoTrackingEnable
        minuteLabel2.isEnabled = timer.isAutoTrackingEnable

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

        inactivityHourInputField = JBTextField((if (inactivityHours < 10) "0" else "") + inactivityHours.toString())
        inactivityMinutesInputField = JBTextField((if (inactivityMinutes < 10) "0" else "") + inactivityMinutes.toString())


        inactivityTimePanel.add(inactivityHourInputField)
        inactivityTimePanel.add(hourLabel1)
        inactivityTimePanel.add(inactivityMinutesInputField)
        inactivityTimePanel.add(minuteLabel1)

        inactivityHourInputField.isEnabled = timer.isAutoTrackingEnable
        inactivityMinutesInputField.isEnabled = timer.isAutoTrackingEnable
        inactivityTextField.isEnabled = timer.isAutoTrackingEnable
        hourLabel1.isEnabled = timer.isAutoTrackingEnable
        minuteLabel1.isEnabled = timer.isAutoTrackingEnable


        val inactivityPeriodPanel = JPanel(FlowLayout(3))

        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.1 * myWidth).toInt() - 2, (0.0875 * height).toInt())
        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(inactivityTimePanel)

        return inactivityPeriodPanel
    }


    private fun isTrackingModeChanged(autoTrackEnabled: Boolean, manualTrackEnabled: Boolean, noTrackingEnabled: Boolean) {

        scheduledHour.isEnabled = autoTrackEnabled
        scheduledMinutes.isEnabled = autoTrackEnabled

        inactivityHourInputField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        inactivityMinutesInputField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        inactivityTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        hourLabel1.isEnabled = autoTrackEnabled && !noTrackingEnabled
        minuteLabel1.isEnabled = autoTrackEnabled && !noTrackingEnabled

        scheduledTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled
        isScheduledCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        hourLabel2.isEnabled = autoTrackEnabled && !noTrackingEnabled
        minuteLabel2.isEnabled = autoTrackEnabled && !noTrackingEnabled

        postWhenProjectClosedCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        postWhenProjectClosedTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled

        postWhenCommitCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        postWhenCommitTextField.isEnabled = autoTrackEnabled && !noTrackingEnabled

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