package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTrackerSettingsTab
import com.github.jk1.ytplugin.timeTracker.TimeTrackingService
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.github.jk1.ytplugin.ui.HyperlinkLabel
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.ui.components.*
import com.intellij.util.net.HttpConfigurable
import org.jdesktop.swingx.VerticalLayout
import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.net.URL
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


open class SetupDialog(override val project: Project, val repo: YouTrackServer, val fromTracker: Boolean) : DialogWrapper(project, false), ComponentAware {

    private lateinit var notifyFieldLabel: JBLabel

    private val okButton = JButton("Save")
    private val cancelButton = JButton("Cancel")

    private var mainPane = JBTabbedPane()

    private lateinit var tokenLabel: JBLabel
    private lateinit var controlPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var shareUrlCheckBox: JBCheckBox
    private lateinit var useProxyCheckBox: JBCheckBox

    private lateinit var testConnectionButton: JButton
    private lateinit var proxyButton: JButton

    private var inputUrlTextPane = JTextPane()
    private var inputTokenField = JBPasswordField()

    private val repoConnector = SetupRepositoryConnector()
    private val connectedRepository: YouTrackRepository = YouTrackRepository()

    private val myHeight = 450
    private val myWidth = 530
    private val standardHeight = (0.0613 * myHeight).toInt()

    private val timeTrackingTab = TimeTrackerSettingsTab(repo, myHeight, myWidth)

    private var isConnectionTested = false

    override fun init() {
        title = "YouTrack"
        val timer = ComponentAware.of(repo.project).timeTrackerComponent
        if (timer.isRunning) {
            StopTrackerAction().stopTimer(project)
            timer.isAutoTrackingTemporaryDisabled = true
        }
        rootPane.defaultButton = okButton
        setResizable(false)

        super.init()
    }

    override fun show() {
        init()
        super.show()
    }

    override fun doCancelAction() {
        TimeTrackingService().setupTimeTracking(timeTrackingTab, project)
        super.doCancelAction()
    }

    private fun appendToPane(tp: JTextPane, msg: String, c: Color) {
        val sc = StyleContext.getDefaultStyleContext()
        var aset: AttributeSet? = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c)
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
        tp.caretPosition = tp.document.length
        tp.setCharacterAttributes(aset, false)
        tp.replaceSelection(msg)
    }

    private fun createServerPane(): JPanel {
        val serverUrlLabel = JBLabel("Server URL:")
        inputUrlTextPane.apply {
            layout = BorderLayout()
            border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
            text = repo.url
            background = inputTokenField.background
            // reset the default text area behavior to make TAB key transfer focus
            setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null)
            setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null)
            // make text area border behave similar to the one of the text field
            fun installDefaultBorder() {
                border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBTabbedPane().background, 2),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                                BorderFactory.createEmptyBorder(0, 5, 2, 2)
                        )
                )
            }
            installDefaultBorder()
            addFocusListener(object : FocusListener {
                override fun focusLost(e: FocusEvent) {
                    installDefaultBorder()
                    repaint()
                }

                override fun focusGained(e: FocusEvent) {
                    border = BorderFactory.createCompoundBorder(
                            DarculaTextBorder(),
                            BorderFactory.createEmptyBorder(0, 5, 0, 0))
                    repaint()
                }
            })
            preferredSize = Dimension(375, 28)
        }

        val serverPanel = JPanel(FlowLayout(2))
        serverPanel.add(serverUrlLabel)
        serverPanel.add(inputUrlTextPane)
        serverPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 2)
        return serverPanel
    }

    private fun createTokenPane(): JPanel {
        tokenLabel = JBLabel("Permanent token:")
        inputTokenField.apply {
            text = repo.password
            echoChar = '\u25CF'
            preferredSize = Dimension(375, 31)
        }
        val tokenPanel = JPanel()
        tokenPanel.add(tokenLabel)
        tokenPanel.add(inputTokenField)

        return tokenPanel
    }

    private fun createCheckboxesPane(): JPanel {
        shareUrlCheckBox = JBCheckBox("Share URL", repo.getRepo().isShared)
        useProxyCheckBox = JBCheckBox("Use proxy", repo.getRepo().isUseProxy)

        val checkboxesPanel = JPanel(FlowLayout(2))
        checkboxesPanel.add(shareUrlCheckBox)
        checkboxesPanel.add(useProxyCheckBox)
        checkboxesPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
        return checkboxesPanel
    }

    private fun createAdvertiserPane(): JPanel {
        val advertiserLabel = HyperlinkLabel("Get YouTrack",
                "https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration")
        val advertiserPane = JPanel(FlowLayout(FlowLayout.RIGHT))
        advertiserPane.add(advertiserLabel)
        advertiserPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
        return advertiserPane
    }

    private fun createInfoPane(): JPanel {
        val getTokenInfoLabel = HyperlinkLabel("Learn how to generate a permanent token",
                "https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html")

        val tokenInfoPane = JPanel(FlowLayout(FlowLayout.RIGHT))
        tokenInfoPane.add(getTokenInfoLabel)
        tokenInfoPane.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
        return tokenInfoPane
    }

    private fun createNotifierPane(): JPanel {

        notifyFieldLabel = JBLabel("").apply { foreground = Color.red }
        val notifyPane = JPanel(BorderLayout())
        notifyPane.add(notifyFieldLabel, BorderLayout.CENTER)

        return notifyPane
    }

    private fun prepareTabbedPane(): JBTabbedPane {

        controlPanel = JBPanel<JBPanelWithEmptyText>().apply { layout = null }

        val connectionTab = JBPanel<JBPanelWithEmptyText>().apply {
            layout = GridLayout(9, 1)
            add(createAdvertiserPane())
            add(createServerPane())
            add(createCheckboxesPane())
            add(createTokenPane())
            add(createInfoPane())
            add(createNotifierPane())
        }

        mainPane.apply {
            addTab("General", null, connectionTab, null)
            addTab("Time Tracking", null, timeTrackingTab, null)
            setMnemonicAt(0, KeyEvent.VK_1)
            selectedIndex = if (fromTracker) 1 else 0
        }

        if (!repo.getRepo().isConfigured) {
            timeTrackingTab.forbidSelection()
        } else {
            timeTrackingTab.allowSelection()
        }

        return mainPane
    }

    private fun drawAutoCorrection(fontColor: Color) {
        fun getColor(closure: () -> Boolean) = if (closure.invoke()) fontColor else Color.GREEN
        if (repoConnector.noteState == NotifierState.SUCCESS) {
            logger.info("YouTrack repository ${connectedRepository.url} connected")
            val oldAddress = inputUrlTextPane.text
            // if we managed to fix this and there's no protocol, well, it must be a default one missing
            val oldUrl = URL(if (oldAddress.startsWith("http")) oldAddress else "http://$oldAddress")
            val fixedUrl = URL(connectedRepository.url)
            inputUrlTextPane.text = ""
            val protocolColor = getColor { oldUrl.protocol == fixedUrl.protocol && oldAddress.startsWith("http") }
            appendToPane(inputUrlTextPane, fixedUrl.protocol, protocolColor)
            appendToPane(inputUrlTextPane, "://", protocolColor)
            appendToPane(inputUrlTextPane, fixedUrl.host, getColor { oldUrl.host == fixedUrl.host })
            if (fixedUrl.port != -1) {
                val color = getColor { oldUrl.port == fixedUrl.port }
                appendToPane(inputUrlTextPane, ":", color)
                appendToPane(inputUrlTextPane, fixedUrl.port.toString(), color)
            }
            if (fixedUrl.path.isNotEmpty()) {
                appendToPane(inputUrlTextPane, fixedUrl.path, getColor { oldUrl.path == fixedUrl.path })
            }
        }
    }

    override fun createActions(): Array<out Action> =
            arrayOf()

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
        button.actionMap.put("apply", action)
        return button
    }

    private fun createButtonsPanel(): JPanel {
        val buttonsPanel = JPanel(FlowLayout(2))
        testConnectionButton = JButton("Test Connection")
        testConnectionButton.addActionListener { testConnectionAction() }
        testConnectionButton.preferredSize = Dimension((0.285 * myWidth).toInt(), standardHeight)

        proxyButton = JButton("Proxy settings...")
        proxyButton.addActionListener { HttpConfigurable.editConfigurable(controlPanel) }
        proxyButton.preferredSize = Dimension((0.285 * myWidth).toInt(), standardHeight)

        okButton.addActionListener { okAction() }
        okButton.preferredSize = Dimension((0.17 * myWidth).toInt(), standardHeight)

        cancelButton.addActionListener { doCancelAction() }
        cancelButton.preferredSize = Dimension((0.17 * myWidth).toInt(), standardHeight)

        val sep = JLabel("")
        sep.preferredSize = Dimension(1, standardHeight)
        buttonsPanel.add(testConnectionButton)
        buttonsPanel.add(proxyButton)
        buttonsPanel.add(cancelButton)
        buttonsPanel.add(okButton)
        buttonsPanel.add(sep)

        if (fromTracker) {
            proxyButton.isVisible = false
            testConnectionButton.isVisible = false
        }

        return buttonsPanel
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(VerticalLayout())
        val tabbedPane = prepareTabbedPane()
        contextPane.apply {
            preferredSize = Dimension(myWidth, myHeight)
            minimumSize = preferredSize
            add(tabbedPane)
            add(createButtonsPanel())
        }
        mainPane.addChangeListener {
            if (mainPane.selectedIndex == 1) {
                proxyButton.isVisible = false
                testConnectionButton.isVisible = false
            } else {
                proxyButton.isVisible = true
                testConnectionButton.isVisible = true
            }
        }

        return contextPane
    }

    private fun testConnectionAction() {

        val isRememberPassword = PasswordSafe.instance.isRememberPasswordByDefault
        if (!isRememberPassword) {
            repoConnector.noteState = NotifierState.PASSWORD_NOT_STORED
        }
        val fontColor = inputTokenField.foreground

        // current implementation allows to login with empty password (as guest) but we do not want to allow it
        if (inputUrlTextPane.text.isNotBlank() && inputTokenField.password.isNotEmpty()) {

            val myRepositoryType = YouTrackRepositoryType()
            connectedRepository.isLoginAnonymously = false

            connectedRepository.url = if (inputUrlTextPane.text.startsWith("http"))
                inputUrlTextPane.text else "http://${inputUrlTextPane.text}"
            connectedRepository.password = String(inputTokenField.password)
            connectedRepository.username = "random" // ignored by YouTrack anyway when token is sent as password
            connectedRepository.repositoryType = myRepositoryType
            connectedRepository.storeCredentials()

            connectedRepository.isShared = shareUrlCheckBox.isSelected

            val proxy = HttpConfigurable.getInstance()
            if (proxy.PROXY_HOST != null || !useProxyCheckBox.isSelected) {
                connectedRepository.isUseProxy = useProxyCheckBox.isSelected
                if (inputUrlTextPane.text.isNotEmpty() && inputTokenField.password.isNotEmpty()) {
                    repoConnector.testConnection(connectedRepository, project)
                }
            } else {
                repoConnector.noteState = NotifierState.NULL_PROXY_HOST
                connectedRepository.isUseProxy = false
            }
        }
        drawAutoCorrection(fontColor)

        if (inputUrlTextPane.text.isBlank() || inputTokenField.password.isEmpty()) {
            repoConnector.noteState = NotifierState.EMPTY_FIELD
        } else if (!repoConnector.isValidToken(connectedRepository.password)) {
            repoConnector.noteState = NotifierState.INVALID_TOKEN
        } else if (PasswordSafe.instance.isMemoryOnly) {
            repoConnector.noteState = NotifierState.PASSWORD_NOT_STORED
        }

        if (repoConnector.noteState != NotifierState.SUCCESS) {
            timeTrackingTab.forbidSelection()
        } else {
            timeTrackingTab.allowSelection()
        }

        repoConnector.setNotifier(notifyFieldLabel)

        isConnectionTested = true
    }

    private fun okAction() {

        if (!isConnectionTested) {
            testConnectionAction()
        }

        // current implementation allows to login with empty password (as guest) but we do not want to allow it
        if (repoConnector.noteState != NotifierState.EMPTY_FIELD) {
            val myRepository: YouTrackRepository = repo.getRepo()
            myRepository.isLoginAnonymously = false

            myRepository.url = connectedRepository.url
            myRepository.password = String(inputTokenField.password)
            myRepository.username = connectedRepository.username
            myRepository.repositoryType = connectedRepository.repositoryType
            myRepository.storeCredentials()

            myRepository.isShared = connectedRepository.isShared
            myRepository.isUseProxy = connectedRepository.isUseProxy

            if (repoConnector.noteState == NotifierState.SUCCESS) {
                repoConnector.showIssuesForConnectedRepo(myRepository, project)
            }

            TimeTrackingService().setupTimeTracking(timeTrackingTab, project)

        }

        if (repoConnector.noteState != NotifierState.NULL_PROXY_HOST && repoConnector.noteState !=
                NotifierState.PASSWORD_NOT_STORED && repoConnector.noteState != NotifierState.EMPTY_FIELD) {
            this@SetupDialog.close(0)
        }
    }

}

