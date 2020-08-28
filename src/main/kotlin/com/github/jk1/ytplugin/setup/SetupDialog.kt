package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.HyperlinkLabel
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.ui.components.*
import com.intellij.util.net.HttpConfigurable
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.net.URL
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


open class SetupDialog(override val project: Project, val repo: YouTrackServer) : DialogWrapper(project, false), ComponentAware {

    private lateinit var notifyFieldLabel: JBLabel

    private lateinit var tokenLabel: JBLabel
    private lateinit var controlPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var shareUrlCheckBox: JBCheckBox
    private lateinit var useProxyCheckBox: JBCheckBox

    private var inputUrlTextPane = JTextPane()
    private var inputTokenField = JBPasswordField()

    val repoConnector = SetupRepositoryConnector()
    private val connectedRepository: YouTrackRepository = YouTrackRepository()

    private var isConnectionTested = false

    override fun init() {
        title = "YouTrack"
        super.init()
    }

    override fun show() {
        init()
        super.show()
    }

    private fun testConnectionAction() {
        val fontColor = inputTokenField.foreground

        val myRepositoryType = YouTrackRepositoryType()
        connectedRepository.url = inputUrlTextPane.text
        connectedRepository.password = String(inputTokenField.password)
        connectedRepository.username = "Token-based authentication" // ignored by YouTrack anyway when token is sent as password
        connectedRepository.repositoryType = myRepositoryType
        connectedRepository.storeCredentials()

        connectedRepository.isShared = shareUrlCheckBox.isSelected
        connectedRepository.isLoginAnonymously = false

        val proxy = HttpConfigurable.getInstance()
        if (proxy.PROXY_HOST != null || !useProxyCheckBox.isSelected) {
            connectedRepository.isUseProxy = useProxyCheckBox.isSelected
            if(inputUrlTextPane.text.isNotEmpty() && inputTokenField.password.isNotEmpty()){
                repoConnector.testConnection(connectedRepository, project)
            }
        } else {
            repoConnector.noteState = NotifierState.NULL_PROXY_HOST
            connectedRepository.isUseProxy = false
        }

        drawAutoCorrection(fontColor)

        if (inputUrlTextPane.text.isBlank() || inputTokenField.password.isEmpty()) {
            repoConnector.noteState = NotifierState.EMPTY_FIELD
        }  else if (!repoConnector.isValidToken(connectedRepository.password)) {
            repoConnector.noteState = NotifierState.INVALID_TOKEN
        }

        repoConnector.setNotifier(notifyFieldLabel)
        isConnectionTested = true
    }

    private fun appendToPane(tp: JTextPane, msg: String, c: Color) {
        val sc = StyleContext.getDefaultStyleContext()
        var aset: AttributeSet? = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c)
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
        tp.caretPosition = tp.document.length
        tp.setCharacterAttributes(aset, false)
        tp.replaceSelection(msg)
    }

    private fun enableButtons() {
        this.useProxyCheckBox.isEnabled = HttpConfigurable.getInstance().USE_HTTP_PROXY
        if (!HttpConfigurable.getInstance().USE_HTTP_PROXY) {
            this.useProxyCheckBox.isSelected = false
        }
    }

    private fun prepareTabbedPane(): JTabbedPane {
        val serverUrlLabel = JBLabel("Server URL:")
        serverUrlLabel.setBounds(65, 61, 100, 22)
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
            setBounds(152, 60, 374, 28)
        }

        tokenLabel = JBLabel("Permanent Token:")
        tokenLabel.setBounds(15, 122, 150, 22)
        inputTokenField.apply {
            text = repo.password
            echoChar = '\u25CF'
            setBounds(150, 120, 378, 31)
        }

        val advertiserLabel = HyperlinkLabel("Not YouTrack customer yet? Get YouTrack",
                "https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration")
        advertiserLabel.setBounds(240, 30, 300, 17)

        val getTokenInfoLabel = HyperlinkLabel("Get token",
                "https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html")
        getTokenInfoLabel.setBounds(457, 155, 100, 17)

        notifyFieldLabel = JBLabel("").apply {
            foreground = Color.red
            setBounds(150, 158, 400, 36)
        }

        shareUrlCheckBox = JBCheckBox("Share Url", repo.getRepo().isShared)
        shareUrlCheckBox.setBounds(440, 95, 100, 17)

        useProxyCheckBox = JBCheckBox("Use Proxy", repo.getRepo().isUseProxy)
        useProxyCheckBox.setBounds(330, 95, 100, 17)

        controlPanel = JBPanel<JBPanelWithEmptyText>().apply { layout = null }

        val connectionTab = JBPanel<JBPanelWithEmptyText>().apply {
            setBounds(100, 100, 580, 300)
            layout = null
            add(shareUrlCheckBox)
            add(advertiserLabel)
            add(serverUrlLabel)
            add(inputUrlTextPane)
            add(tokenLabel)
            add(inputTokenField)
            add(getTokenInfoLabel)
            add(notifyFieldLabel)
            add(useProxyCheckBox)
        }

        return JBTabbedPane().apply {
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            addTab("General", null, connectionTab, null)
            setMnemonicAt(0, KeyEvent.VK_1)
        }
    }

    private fun drawAutoCorrection(fontColor: Color) {
        fun getColor(closure: () -> Boolean) = if (closure.invoke()) fontColor else Color.GREEN
        if (repoConnector.noteState == NotifierState.SUCCESS)  {
            logger.info("YouTrack repository ${connectedRepository.url} connected")
            val oldAddress = inputUrlTextPane.text
            // if we managed to fix this and there's no protocol, well, it must be a default one missing
            val oldUrl = URL(if (oldAddress.startsWith("http")) oldAddress else  "http://$oldAddress")
            val fixedUrl = URL(connectedRepository.url)
            inputUrlTextPane.text = ""
            val protocolColor = getColor { oldUrl.protocol == fixedUrl.protocol }
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
            arrayOf(TestConnectionAction(), OpenProxySettingsAction(), OkAction("Ok"), cancelAction)

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
        button.actionMap.put("apply", action)
        return button
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(GridLayout())
        val tabbedPane = prepareTabbedPane()
        contextPane.apply {
            preferredSize = Dimension(540, 230)
            minimumSize = preferredSize
            add(tabbedPane)
        }
        return contextPane
    }

    inner class OkAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {

            if (!isConnectionTested) {
                testConnectionAction()
            }

            val myRepository: YouTrackRepository = repo.getRepo()
            myRepository.url = connectedRepository.url
            myRepository.password = String(inputTokenField.password)
            myRepository.username = connectedRepository.username
            myRepository.repositoryType = connectedRepository.repositoryType
            myRepository.storeCredentials()

            myRepository.isShared = connectedRepository.isShared
            myRepository.isUseProxy = connectedRepository.isUseProxy
            myRepository.isLoginAnonymously = false

            repoConnector.showIssuesForConnectedRepo(myRepository, project)

            if (repoConnector.noteState != NotifierState.NULL_PROXY_HOST){
                this@SetupDialog.close(0)
            }
        }
    }

    inner class TestConnectionAction : AbstractAction("Test Connection") {
        override fun actionPerformed(e: ActionEvent) {
            testConnectionAction()
        }
    }

    inner class OpenProxySettingsAction : AbstractAction("Proxy settings...") {
        override fun actionPerformed(e: ActionEvent) {
            HttpConfigurable.editConfigurable(controlPanel)
            enableButtons()
        }
    }
}

