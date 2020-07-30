package com.github.jk1.ytplugin.setupWindow

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.ui.HyperlinkLabel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.ui.components.*
import com.intellij.util.net.HttpConfigurable
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


open class SetupDialog(override val project: Project) : DialogWrapper(project, false), ComponentAware {

    private val testConnectionAction = TestConnectionCommandAction("Test Connection")
    private val okAction = OkCommandAction("Ok")

    private lateinit var bigTabFrame: JBTabbedPane
    private lateinit var tabPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var tab2Panel: JBPanel<JBPanelWithEmptyText>

    private lateinit var serverUrl: JBLabel
    private lateinit var notifyField: JBLabel
    private lateinit var proxyDescription: JBLabel

    private lateinit var tokenField: JBLabel
    private lateinit var getTokenField: HyperlinkLabel
    private lateinit var advertiserField: HyperlinkLabel
    private lateinit var controlPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var shareUrl: JBCheckBox
    private lateinit var useProxy: JBCheckBox
    private lateinit var useHTTP: JBCheckBox
    private lateinit var loginAnon: JBCheckBox
    private lateinit var testConnectPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var proxyPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var okPanel: JBPanel<JBPanelWithEmptyText>
    private lateinit var cancelPanel: JBPanel<JBPanelWithEmptyText>
    lateinit var myRepository: YouTrackRepository

    private var proxySettingsButton = JButton("Proxy settings...")
    var inputUrl = JTextPane()
    var inputToken = JBPasswordField()

    override fun init() {
        title = "YouTrack"
        super.init()
    }

    override fun show() {
        init()
        super.show()
    }

    private fun loginAnonymouslyChanged(enabled: Boolean) {
        inputToken.isEnabled = enabled
        tokenField.isEnabled = enabled
        useHTTP.isEnabled = enabled
    }

    private fun testConnectionAction() {
        val setup = SetupTask()
        setup.correctUrl = inputUrl.text
        val fontColor = inputToken.foreground

        myRepository = YouTrackRepository()
        val myRepositoryType = YouTrackRepositoryType()

        myRepository.url = inputUrl.text
        myRepository.password = inputToken.text
        myRepository.username = "random" // could be anything
        myRepository.repositoryType = myRepositoryType
        myRepository.storeCredentials()

        myRepository.isShared = shareUrl.isSelected()
        myRepository.isUseProxy = useProxy.isSelected()
        myRepository.isUseHttpAuthentication = useHTTP.isSelected()
        myRepository.isLoginAnonymously = loginAnon.isSelected()

        setup.testConnection(myRepository, project)

        val oldUrl = inputUrl.text
        inputUrl.text = ""

        if (oldUrl == setup.correctUrl) {
            println("here")
            inputUrl.text = oldUrl
        } else {
            if (!oldUrl.contains("/youtrack") && setup.correctUrl.contains("/youtrack")) {
                if (!oldUrl.contains("https") && oldUrl.contains("http") && setup.correctUrl.contains("https")) {
                    appendToPane(inputUrl, "https", Color.GREEN)
                    appendToPane(inputUrl, setup.correctUrl.substring(5, setup.correctUrl.length - 9), fontColor)
                    appendToPane(inputUrl, "/youtrack", Color.GREEN)
                }
                else{
                    appendToPane(inputUrl, setup.correctUrl.substring(0, setup.correctUrl.length - 9), fontColor)
                    appendToPane(inputUrl, "/youtrack", Color.GREEN)
                }
            }
            else {
                if (!oldUrl.contains("https") && oldUrl.contains("http") && setup.correctUrl.contains("https")) {
                    appendToPane(inputUrl, "https", Color.GREEN)
                    appendToPane(inputUrl, setup.correctUrl.substring(5, setup.correctUrl.length), fontColor)
                } else {
                    inputUrl.text = oldUrl
                }
            }
        }

        if (myRepository.url.isBlank() || myRepository.password.isBlank()) {
            notifyField.foreground = Color.red
            notifyField.text = "Url and token fields are mandatory"
        } else if (myRepository.isLoginAnonymously && setup.noteState != NotifierState.UNKNOWN_HOST) {
            notifyField.foreground = Color.green
            notifyField.text = "Login as a guest"
        } else
            setup.setNotifier(notifyField)

        if (setup.noteState == NotifierState.SUCCESS || myRepository.isLoginAnonymously){
            val setupWindow = SetupWindowManager(project)
            setupWindow.showIssues(myRepository)

        }
    }

    private fun appendToPane(tp: JTextPane, msg: String, c: Color) {
        val sc = StyleContext.getDefaultStyleContext()
        var aset: AttributeSet? = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c)
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
        val len = tp.document.length
        tp.caretPosition = len
        tp.setCharacterAttributes(aset, false)
        tp.replaceSelection(msg)
    }

    private fun enableButtons() {
        this.useProxy.isEnabled = HttpConfigurable.getInstance().USE_HTTP_PROXY
        if (!HttpConfigurable.getInstance().USE_HTTP_PROXY) {
            this.useProxy.isSelected = false
        }
    }

    private fun prepareTabbedPane(): JTabbedPane {

//        val color = (inputToken.border as LineBorder?)?.lineColor
//        val size = (inputToken.border as LineBorder?)?.thickness
        serverUrl = JBLabel("Server Url:")
        serverUrl.setBounds(65, 60, 100, 22)
        inputUrl.apply {
            layout = BorderLayout()

            border = inputToken.border
//            border = MatteBorder(size!!, size, size, size, Color.red)
            background = inputToken.background
            setBounds(152, 60, 374, 24)

        }


        tokenField = JBLabel("Permanent token:")
        tokenField.setBounds(15, 120, 150, 22)
        inputToken.apply {
            echoChar = '\u25CF'
            setBounds(150, 120, 378, 31)
        }

        advertiserField = HyperlinkLabel("Not YouTrack customer yet? Get YouTrack", "https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration")
        advertiserField.setBounds(240, 30, 300, 17)

        getTokenField = HyperlinkLabel("Get token", "https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html")
        getTokenField.setBounds(457, 155, 100, 17)

        notifyField = JBLabel("").apply {
            foreground = Color.red;
            setBounds(150, 158, 250, 17)
        }
        shareUrl = JBCheckBox("Share Url", false)
        shareUrl.setBounds(440, 95, 100, 17)

        loginAnon = JBCheckBox("Login Anonymously", false)
        loginAnon.setBounds(150, 95, 170, 17)

        useHTTP = JBCheckBox("Use HTTP", false)
        useHTTP.setBounds(20, 50, 100, 17);

        useProxy = JBCheckBox("Use Proxy", false)
        useProxy.setBounds(20, 100, 100, 17)

        proxyDescription = JBLabel("You can configure the HTTP Proxy to:")
        proxyDescription.setBounds(220, 20, 370, 20)


        proxySettingsButton.addActionListener(ActionListener {
            HttpConfigurable.editConfigurable(controlPanel)
            enableButtons()
        })

        loginAnon.addActionListener(ActionListener { loginAnonymouslyChanged(!loginAnon.isSelected()) })

        proxyPanel = JBPanel<JBPanelWithEmptyText>().apply {
            add(proxySettingsButton)
            setBounds(15, 155, 140, 40)
        }

        controlPanel = JBPanel<JBPanelWithEmptyText>().apply { layout = null }

        tabPanel = JBPanel<JBPanelWithEmptyText>().apply{
            setBounds(100, 100, 580, 300);
            layout = null
            add(shareUrl)
            add(advertiserField)
            add(loginAnon)
            add(serverUrl)
            add(inputUrl)
            add(tokenField)
            add(inputToken)
            add(getTokenField)
            add(notifyField)
            pack()
    }

        tab2Panel = JBPanel<JBPanelWithEmptyText>().apply {
            setBounds(100, 100, 580, 300);
            layout = null
            add(proxyDescription)
            add(useProxy)
            add(useHTTP)
            add(proxyPanel)
            pack()
        }

        bigTabFrame = JBTabbedPane().apply {
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            addTab("General", null, tabPanel, null);
            setMnemonicAt(0, KeyEvent.VK_1)
            addTab("Proxy settings", null, tab2Panel, null);
            setMnemonicAt(1, KeyEvent.VK_2)
            pack()

        }
        return bigTabFrame
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
        val toolkit: Toolkit = Toolkit.getDefaultToolkit()
        val screenSize: Dimension = toolkit.screenSize
        contextPane.apply{
            add(tabbedPane)
            title = "YouTrack"
            preferredSize = Dimension(540, 230)
            minimumSize = Dimension(540, 230)
            setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2)
            pack()
        }
        return contextPane
    }

    /**
     * Submits command for async execution and closes command dialog immediately
     */
    inner class OkCommandAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            testConnectionAction()
            this@SetupDialog.close(0)
        }
    }

    /**
     * Submits command for async execution
     */
    inner class TestConnectionCommandAction(name: String) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            testConnectionAction()
        }
    }
}

