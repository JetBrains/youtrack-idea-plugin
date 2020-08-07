package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.ui.HyperlinkLabel
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.ui.components.*
import com.intellij.util.net.HttpConfigurable
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


open class SetupDialog(override val project: Project, inputRepository: YouTrackServer) : DialogWrapper(project, false), ComponentAware {


    private val testConnectionAction = TestConnectionCommandAction("Test Connection")
    private val okAction = OkCommandAction("Ok")

    private lateinit var myTabbedPane: JBTabbedPane
    private lateinit var tabPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var tab2Panel: JBPanel<JBPanelWithEmptyText>

    private lateinit var serverUrlLabel: JBLabel
    private lateinit var notifyFieldLabel: JBLabel
    private lateinit var notifyFieldTab2Label: JBLabel

    private lateinit var proxyDescriptionLabel: JBLabel

    private lateinit var tokenLabel: JBLabel
    private lateinit var getTokenInfoLabel: HyperlinkLabel
    private lateinit var advertiserLabel: HyperlinkLabel
    private lateinit var controlPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var shareUrlCheckBox: JBCheckBox
    private lateinit var useProxyCheckBox: JBCheckBox
    private lateinit var useHTTPCheckBox: JBCheckBox
    private lateinit var loginAnonCheckBox: JBCheckBox
    private lateinit var proxyPanel: JBPanel<JBPanelWithEmptyText>

    private val repo: YouTrackServer = inputRepository
    private var myRepository: YouTrackRepository = repo.getRepo()

    private var proxySettingsButton = JButton("Proxy settings...")
    var inputUrlTextPane = JTextPane()
    var inputTokenField = JBPasswordField()

    override fun init() {
        title = "YouTrack"
        super.init()
    }

    override fun show() {
        init()
        super.show()
    }

    private fun loginAnonymouslyChanged(enabled: Boolean) {
        inputTokenField.isEnabled = enabled
        tokenLabel.isEnabled = enabled
        useHTTPCheckBox.isEnabled = enabled
    }


    private fun testConnectionAction() {
        val repoConnector = SetupRepositoryConnector()
        repoConnector.correctUrl = inputUrlTextPane.text
        val fontColor = inputTokenField.foreground

        val myRepositoryType = YouTrackRepositoryType()

        myRepository.url = inputUrlTextPane.text
        myRepository.password = inputTokenField.text
        myRepository.username = "random" // could be anything
        myRepository.repositoryType = myRepositoryType
        myRepository.storeCredentials()

        myRepository.isShared = shareUrlCheckBox.isSelected
        myRepository.isUseProxy = useProxyCheckBox.isSelected
        myRepository.isUseHttpAuthentication = useHTTPCheckBox.isSelected
        myRepository.isLoginAnonymously = loginAnonCheckBox.isSelected

        repoConnector.testConnection(myRepository, project)

        val oldUrl = inputUrlTextPane.text
        inputUrlTextPane.text = ""

        if (oldUrl == repoConnector.correctUrl) {
            inputUrlTextPane.text = oldUrl
        } else {
            if (!oldUrl.contains("/youtrack") && repoConnector.noteState == NotifierState.SUCCESS) {
                if (!oldUrl.contains("https") && oldUrl.contains("http") && repoConnector.correctUrl.contains("https")) {
                    appendToPane(inputUrlTextPane, "https", Color.GREEN)
                    appendToPane(inputUrlTextPane, repoConnector.correctUrl.substring(5, repoConnector.correctUrl.length - 9), fontColor)
                    appendToPane(inputUrlTextPane, "/youtrack", Color.GREEN)
                }
                else{
                    appendToPane(inputUrlTextPane, repoConnector.correctUrl.substring(0, repoConnector.correctUrl.length - 9), fontColor)
                    appendToPane(inputUrlTextPane, "/youtrack", Color.GREEN)
                }
            }
            else {
                if (!oldUrl.contains("https") && oldUrl.contains("http") && repoConnector.correctUrl.contains("https")) {
                    appendToPane(inputUrlTextPane, "https", Color.GREEN)
                    appendToPane(inputUrlTextPane, repoConnector.correctUrl.substring(5, repoConnector.correctUrl.length), fontColor)
                } else {
                    inputUrlTextPane.text = oldUrl
                }
            }
        }

        if (myRepository.url.isBlank() || myRepository.password.isBlank()) {
            notifyFieldLabel.foreground = Color.red
            notifyFieldLabel.text = "Url and token fields are mandatory"
        } else if (myRepository.isLoginAnonymously && repoConnector.noteState != NotifierState.UNKNOWN_HOST) {
            notifyFieldLabel.foreground = Color.green
            notifyFieldLabel.text = "Login as a guest"
        } else
            repoConnector.setNotifier(notifyFieldLabel)

        notifyFieldTab2Label.text = notifyFieldLabel.text
        notifyFieldTab2Label.foreground = notifyFieldLabel.foreground

        if (repoConnector.noteState == NotifierState.SUCCESS || myRepository.isLoginAnonymously){
            repoConnector.showIssuesForConnectedRepo(myRepository, project)

        }
        myRepository.url = repoConnector.correctUrl
        myRepository.password = inputTokenField.text
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
        serverUrlLabel = JBLabel("Server URL:")
        serverUrlLabel.setBounds(65, 60, 100, 22)
        inputUrlTextPane.apply {
            layout = BorderLayout()
            border = object: DarculaTextBorder() {
                override fun paddings(): Insets {
                    return JBUI.emptyInsets()
                }
            }
            text = repo.url
            background = inputTokenField.background
            setBounds(152, 60, 374, 24)
        }

        tokenLabel = JBLabel("Permanent Token:")
        tokenLabel.setBounds(15, 120, 150, 22)
        inputTokenField.apply {
            text = repo.password
            echoChar = '\u25CF'
            setBounds(150, 120, 378, 31)
        }

        advertiserLabel = HyperlinkLabel("Not YouTrack customer yet? Get YouTrack", "https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration")
        advertiserLabel.setBounds(240, 30, 300, 17)

        getTokenInfoLabel = HyperlinkLabel("Get token", "https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html")
        getTokenInfoLabel.setBounds(457, 155, 100, 17)

        notifyFieldLabel = JBLabel("").apply {
            foreground = Color.red;
            setBounds(150, 158, 250, 36)
        }

        notifyFieldTab2Label = JBLabel("").apply {
            foreground = Color.red;
            setBounds(220, 160, 250, 36)
        }

        shareUrlCheckBox = JBCheckBox("Share Url", false)
        shareUrlCheckBox.setBounds(440, 95, 100, 17)

        loginAnonCheckBox = JBCheckBox("Login Anonymously", false)
        loginAnonCheckBox.setBounds(150, 95, 170, 17)

        useHTTPCheckBox = JBCheckBox("Use HTTP", false)
        useHTTPCheckBox.setBounds(20, 50, 100, 17);

        useProxyCheckBox = JBCheckBox("Use Proxy", false)
        useProxyCheckBox.setBounds(20, 100, 100, 17)

        proxyDescriptionLabel = JBLabel("You can configure the HTTP Proxy to:")
        proxyDescriptionLabel.setBounds(220, 20, 370, 20)


        proxySettingsButton.addActionListener(ActionListener {
            HttpConfigurable.editConfigurable(controlPanel)
            enableButtons()
        })

        loginAnonCheckBox.addActionListener { loginAnonymouslyChanged(!loginAnonCheckBox.isSelected) }

        proxyPanel = JBPanel<JBPanelWithEmptyText>().apply {
            add(proxySettingsButton)
            setBounds(15, 155, 140, 40)
        }

        controlPanel = JBPanel<JBPanelWithEmptyText>().apply { layout = null }

        tabPanel = JBPanel<JBPanelWithEmptyText>().apply{
            setBounds(100, 100, 580, 300);
            layout = null
            add(shareUrlCheckBox)
            add(advertiserLabel)
            add(loginAnonCheckBox)
            add(serverUrlLabel)
            add(inputUrlTextPane)
            add(tokenLabel)
            add(inputTokenField)
            add(getTokenInfoLabel)
            add(notifyFieldLabel)
        }

        tab2Panel = JBPanel<JBPanelWithEmptyText>().apply {
            setBounds(100, 100, 580, 300);
            layout = null
            add(proxyDescriptionLabel)
            add(useProxyCheckBox)
            add(useHTTPCheckBox)
            add(proxyPanel)
            add(notifyFieldTab2Label)
        }

        myTabbedPane = JBTabbedPane().apply {
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            addTab("General", null, tabPanel, null);
            setMnemonicAt(0, KeyEvent.VK_1)
            addTab("Proxy settings", null, tab2Panel, null);
            setMnemonicAt(1, KeyEvent.VK_2)

        }
        return myTabbedPane
    }

    override fun createActions(): Array<out Action> = arrayOf(testConnectionAction, okAction, cancelAction)

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
        button.actionMap.put("apply", action)
        return button
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(GridLayout())
        val tabbedPane = prepareTabbedPane()
        contextPane.apply{
            preferredSize = Dimension(540, 230)
            minimumSize = preferredSize
            add(tabbedPane)
        }
        return contextPane
    }

    inner class OkCommandAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            testConnectionAction()
            this@SetupDialog.close(0)
        }
    }

    inner class TestConnectionCommandAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            testConnectionAction()
        }
    }
}

