package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.AnyModalityState
import com.intellij.openapi.ui.ComboBox
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.ui.components.*
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JPanel
import javax.swing.border.EtchedBorder
import javax.swing.border.TitledBorder


class TimeTrackerSettingsTab(var repo: YouTrackServer, myHeight: Int, private val myWidth: Int) : JBPanel<JBPanelWithEmptyText>() {


    private val standardHeight = (0.0613 * myHeight).toInt()

    private var scheduledHour = JBTextField("19")
    private var scheduledMinutes = JBTextField("00")

    private var inactivityHourInputField = JBTextField("00")
    private var inactivityMinutesInputField = JBTextField("10")
    private var inactivityTextField = JBLabel("Inactivity period:")

    private var isScheduledCheckbox = JBCheckBox("On a set schedule at:")

    private var isManualModeRadioButton = JBRadioButton()
    private var manualModeTextField = JBLabel("Manual")

    private var noTrackingButton = JBRadioButton("Off  ")

    private var postWhenProjectClosedCheckbox = JBCheckBox("When closing the project")
    private var postWhenCommitCheckbox = JBCheckBox("When committing changes")

    private var isAutoTrackingEnabledRadioButton = JBRadioButton()
    private var autoTrackingEnabledTextField = JBLabel("Automatic")

    private var commentLabel = JBLabel(" Comment: ")
    private var typeLabel = JBLabel(" Work type:")

    private var commentTextField = PlaceholderTextField("")

    private var hourLabel1 = JBLabel(" hours ")
    private var hourLabel2 = JBLabel(" hours ")
    private var minuteLabel1 = JBLabel(" minutes")
    private var minuteLabel2 = JBLabel(" minutes")

    private val workItemsTypes = arrayOf<String?>("Development")

    private var typeComboBox = ComboBox(workItemsTypes)

    init {
        val timer = ComponentAware.of(repo.project).timeTrackerComponent
        val inactivityPeriodPanel = createInactivityPeriodPanel(timer)
        val trackingModePanel = createTrackingModePanel(timer)
        val schedulePanel = createScheduledPanel(timer)
        val typePanel = createTypePanel(timer)
        val postWhenPanel = createPostWhenPanel(timer)
        val commentPanel = createCommentPanel(timer)

        val autoPanel = JPanel(VerticalLayout(7))
        val loweredEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
        val autoPanelBorder = BorderFactory.createTitledBorder(loweredEtched, "Automatically create work items")
        autoPanelBorder.titlePosition = TitledBorder.TOP
        autoPanel.border = autoPanelBorder
        autoPanel.add(postWhenPanel)
        autoPanel.add(schedulePanel)
        autoPanel.add(inactivityPeriodPanel)

        val preferencesPanel = JPanel(VerticalLayout(7))
        val preferencesPanelBorder = BorderFactory.createTitledBorder(loweredEtched, "Preferences")
        preferencesPanelBorder.titlePosition = TitledBorder.TOP
        preferencesPanel.border = preferencesPanelBorder
        preferencesPanel.add(typePanel)
        preferencesPanel.add(commentPanel)

        layout = GridBagLayout()
        add(trackingModePanel, GridBagConstraints().also {
            it.gridx = 0
            it.fill = GridBagConstraints.HORIZONTAL
        })
        add(autoPanel, GridBagConstraints().also {
            it.gridx = 0
            it.gridheight = 2
            it.fill = GridBagConstraints.HORIZONTAL
        })
        add(preferencesPanel, GridBagConstraints().also {
            it.gridx = 0
            it.gridheight = 3
            it.fill = GridBagConstraints.HORIZONTAL
        })
    }

    private fun createCommentPanel(timer: TimeTracker): JPanel {

        commentTextField = PlaceholderTextField(timer.comment)
        commentTextField.placeholder = "Enter default comment text"
        commentTextField.preferredSize = Dimension(403, standardHeight + 5)
        val commentPanel = JPanel(FlowLayout(2))
        commentLabel.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        commentTextField.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable

        commentPanel.add(commentLabel)
        commentPanel.add(commentTextField)

        return commentPanel
    }

    private fun createTypePanel(timer: TimeTracker): JPanel {
        val typePanel = JPanel(FlowLayout(2))
        typeComboBox.selectedIndex = 0
        typeComboBox.isEditable = true
        typeComboBox.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        typeLabel.isEnabled = timer.isAutoTrackingEnable || timer.isManualTrackingEnable
        typePanel.add(typeLabel)
        typePanel.add(typeComboBox)
        return typePanel
    }

    private fun createPostWhenPanel(timer: TimeTracker): JPanel {

        val postWhenPanel = JPanel(FlowLayout(5))

        postWhenCommitCheckbox.isSelected = timer.isPostAfterCommitEnabled
        postWhenCommitCheckbox.name = "When committing changes"

        postWhenProjectClosedCheckbox.isSelected = timer.isWhenProjectClosedEnabled
        postWhenProjectClosedCheckbox.name = "When closing the project"

        postWhenPanel.add(postWhenProjectClosedCheckbox)

        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.14 * myWidth).toInt(), standardHeight)
        postWhenPanel.add(sep)

        postWhenPanel.add(postWhenCommitCheckbox)

        val sep2 = JBLabel("")
        sep2.preferredSize = Dimension((0.002 * myWidth).toInt(), standardHeight)
        postWhenPanel.add(sep2)

        postWhenProjectClosedCheckbox.isEnabled = timer.isAutoTrackingEnable
        postWhenCommitCheckbox.isEnabled = timer.isAutoTrackingEnable

        return postWhenPanel
    }

    private fun createTrackingModePanel(timer: TimeTracker): JPanel {

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

        if (!repo.getRepo().isConfigured) {
            forbidSelection()
        } else {
            allowSelection(repo)
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
        sep.preferredSize = Dimension((0.213 * myWidth).toInt(), standardHeight)
        val sep2 = JBLabel(" ")
        sep2.preferredSize = Dimension((0.214 * myWidth).toInt(), standardHeight)
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

        isScheduledCheckbox.isSelected = timer.isScheduledEnabled

        timePanel.add(scheduledHour)
        timePanel.add(hourLabel2)
        timePanel.add(scheduledMinutes)
        timePanel.add(minuteLabel2)

        scheduledFieldsEnabling(timer.isAutoTrackingEnable)

        val bigScheduledPanel = JPanel(FlowLayout(5))
        bigScheduledPanel.add(isScheduledCheckbox)
        bigScheduledPanel.add(timePanel)

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
        inactivityFieldsEnabling(timer.isAutoTrackingEnable)

        val inactivityPeriodPanel = JPanel(FlowLayout(3))

        val sep = JBLabel("")
        sep.preferredSize = Dimension((0.017 * myWidth).toInt(), standardHeight)
        inactivityPeriodPanel.add(inactivityTextField)
        inactivityPeriodPanel.add(sep)
        inactivityPeriodPanel.add(inactivityTimePanel)

        return inactivityPeriodPanel
    }

    private fun inactivityFieldsEnabling(enable: Boolean) {
        inactivityHourInputField.isEnabled = enable
        inactivityMinutesInputField.isEnabled = enable
        inactivityTextField.isEnabled = enable
        hourLabel1.isEnabled = enable
        minuteLabel1.isEnabled = enable
    }

    private fun scheduledFieldsEnabling(enable: Boolean) {
        isScheduledCheckbox.isEnabled = enable
        scheduledHour.isEnabled = enable
        scheduledMinutes.isEnabled = enable
        hourLabel2.isEnabled = enable
        minuteLabel2.isEnabled = enable
    }

    private fun isTrackingModeChanged(autoTrackEnabled: Boolean, manualTrackEnabled: Boolean, noTrackingEnabled: Boolean) {

        scheduledHour.isEnabled = autoTrackEnabled
        scheduledMinutes.isEnabled = autoTrackEnabled

        inactivityFieldsEnabling(autoTrackEnabled && !noTrackingEnabled)
        scheduledFieldsEnabling(autoTrackEnabled && !noTrackingEnabled)

        postWhenProjectClosedCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled
        postWhenCommitCheckbox.isEnabled = autoTrackEnabled && !noTrackingEnabled

        commentLabel.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        commentTextField.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        typeLabel.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled
        typeComboBox.isEnabled = (autoTrackEnabled || manualTrackEnabled) && !noTrackingEnabled

    }

    fun forbidSelection() {
        noTrackingButton.isEnabled = false
        isAutoTrackingEnabledRadioButton.isEnabled = false
        isManualModeRadioButton.isEnabled = false
        noTrackingButton.isSelected = true
        isTrackingModeChanged(autoTrackEnabled = false, manualTrackEnabled = false, noTrackingEnabled = false)
    }

    fun allowSelection(repository: YouTrackServer) {
        noTrackingButton.isEnabled = true
        isAutoTrackingEnabledRadioButton.isEnabled = true
        isManualModeRadioButton.isEnabled = true
        val types = TimeTrackingService().getAvailableWorkItemsTypes(repository)
        ApplicationManager.getApplication().invokeLater( {
            var idx = 0
            if (types.isNotEmpty()) {
                typeComboBox.model = DefaultComboBoxModel(types.toTypedArray())
                types.mapIndexed { index, value ->
                    if (value == ComponentAware.of(repo.project).timeTrackerComponent.type) {
                        idx = index
                    }
                }
            }
            typeComboBox.selectedIndex = idx
        }, AnyModalityState.ANY)
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