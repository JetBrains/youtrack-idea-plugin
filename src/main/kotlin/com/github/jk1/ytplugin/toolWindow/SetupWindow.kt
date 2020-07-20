package com.github.jk1.ytplugin.toolWindow

import com.github.jk1.ytplugin.ui.HyperlinkLabel
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.config.RecentTaskRepositories
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.net.HttpConfigurable
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.*
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Class for window for initial Setup of YouTrack
 * @author Akina Boshchenko
 */
class SetupWindow(val project: Project) : ProjectComponent {

    private lateinit var tabFrame: JFrame
    private lateinit var tab2Frame: JFrame
    private lateinit var bigTabFrame: JTabbedPane

    private lateinit var topPanel: JPanel


    private lateinit var mainFrame: JFrame
    private lateinit var serverUrl: JLabel
    private lateinit var notifyField: JLabel
    private lateinit var proxyDescription: JLabel

    private lateinit var tokenField: JLabel
    private lateinit var getTokenField: JLabel
    private lateinit var advertiserField: HyperlinkLabel
    private lateinit var controlPanel: JPanel
    private lateinit var shareUrl: JCheckBox
    private lateinit var useProxy: JCheckBox
    private lateinit var useHTTP: JCheckBox
    private lateinit var loginAnon: JCheckBox
    private lateinit var testConnectPanel: JPanel
    private lateinit var proxyPanel: JPanel
    private lateinit var okPanel:JPanel
    private lateinit var cancelPanel:JPanel

    private var okButton = JButton("OK")
    private var cancelButton = JButton("Cancel")
    private var testConnectButton = JButton("Test connection")
    private var proxySettingsButton = JButton("Proxy settings...")
    private var inputUrl = JTextPane()

    private var inputToken = JPasswordField("")


    init {
        prepareDialogWindow()
    }

    fun showIssues(repository: YouTrackRepository) {
        val myManager:TaskManagerImpl = TaskManager.getManager(project) as TaskManagerImpl
        lateinit var myRepositories: List<YouTrackRepository>
        myRepositories = ArrayList()
        myRepositories.add(repository)
        val newRepositories: List<TaskRepository> = ContainerUtil.map<TaskRepository, TaskRepository>(myRepositories, Function { obj: TaskRepository -> obj.clone() })
        myManager.setRepositories(newRepositories)
        myManager.updateIssues(null)
        RecentTaskRepositories.getInstance().addRepositories(myRepositories)
    }

    fun loginAnonymouslyChanged(enabled: Boolean) {
        inputToken.setEnabled(enabled)
        tokenField.setEnabled(enabled)
        useHTTP.setEnabled(enabled)
    }

    fun testConnectionAction(){
        val setup = SetupTask()

        val myRepository = YouTrackRepository()
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

        setup.testConnection(myRepository, project, notifyField)

        val oldUrl = inputUrl.text
        inputUrl.text = ""

        if (oldUrl == setup.correctUrl){
            inputUrl.text = oldUrl
        }
        else{
            if(!oldUrl.contains("com/youtrack") && setup.correctUrl.contains("com/youtrack")){
                inputUrl.text = oldUrl
                appendToPane(inputUrl, "/youtrack", Color.GREEN)
//                appendToPane(inputUrl, oldUrl.substring(10, oldUrl.length), Color.WHITE)
            }
            if(!oldUrl.contains("https") && setup.correctUrl.contains("https")){
                appendToPane(inputUrl, "https", Color.GREEN)
                appendToPane(inputUrl, oldUrl.substring(4,oldUrl.length), Color.WHITE)
            }
            else{
                inputUrl.text = setup.correctUrl
            }
        }
        showIssues(myRepository)
    }

    private fun appendToPane(tp: JTextPane, msg: String, c: Color) {
        val sc = StyleContext.getDefaultStyleContext()
        var aset: AttributeSet? = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c)
        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console")
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED)
        val len = tp.document.length
        tp.caretPosition = len
        tp.setCharacterAttributes(aset, false)
        tp.replaceSelection(msg)
    }
    private fun prepareDialogWindow() {
        serverUrl = JLabel("Server Url:")
        serverUrl.setBounds(65, 60, 100, 22)
        inputUrl.setBounds(152, 60, 375, 24)
        topPanel = JPanel()
        topPanel.add(inputUrl);


        tokenField = JLabel("Permanent token:")
        tokenField.setBounds(15, 120, 150, 22)
        inputToken.apply {
            setEchoChar('\u25CF')
            setBounds(150, 120, 378, 30)
        }

        advertiserField = HyperlinkLabel("Not YouTrack customer yet? Get YouTrack", "https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration")
        advertiserField.setBounds(240, 30, 300, 17)

        getTokenField = HyperlinkLabel("Get token", "https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html")
        getTokenField.setBounds(457, 155, 100, 17)

        notifyField= JLabel("").apply{
            foreground = Color.red;
            setBounds(150,158, 200, 17)
        }
        shareUrl = JCheckBox("Share Url", false)
        shareUrl.setBounds(440, 90, 100, 17)

        loginAnon = JCheckBox("Login Anonymously", false)
        loginAnon.setBounds(150, 90, 170, 17)

        useHTTP = JCheckBox("Use HTTP", false)
        useHTTP.setBounds(440, 220, 100, 17);

        useProxy = JCheckBox("Use Proxy", false)
        useProxy.setBounds(300, 220, 100, 17)


        proxyDescription = JLabel("You can configure the HTTP Proxy to:" )
        proxyDescription.setBounds(20, 20, 370, 20)

//        You can configure the HTTP Proxy to:
//
//        Only allow content that matches RFC specifications for Web server and clients
//                Restrict the content the Firebox allows into your network, based upon fully a qualified domain name, path name, file name or extension as it appears in the URL.
//        Restrict the content the Firebox allows into your network based upon MIME type.
//        Block downloads of any unique file type, including client-side executable files like Java and ActiveX, by file header (hexadecimal signature) pattern match.
//        Examine the HTTP header to make sure it is not from a known source of suspicious content

        proxySettingsButton.addActionListener(ActionListener {
            HttpConfigurable.editConfigurable(controlPanel)
        })

        loginAnon.addActionListener(ActionListener { loginAnonymouslyChanged(!loginAnon.isSelected()) })

        testConnectButton.addActionListener(ActionListener {
            testConnectionAction()
        })

        testConnectButton.apply {
            setBounds(140, 205, 130, 40)
//            setPreferredSize(Dimension(150, 40))
        }

        testConnectPanel = JPanel().apply {
            add(testConnectButton)
            setBounds(140, 205, 130, 40)
        }

        cancelButton.addActionListener {
            mainFrame.dispose()
        }

        okButton.addActionListener {
            testConnectionAction()
            mainFrame.dispose()
        }

        cancelPanel = JPanel().apply {
            add(cancelButton)
            setBounds(440, 205, 100, 40)
        }

        okPanel = JPanel().apply {
            add(okButton)
            setBounds(350, 205, 100, 40)
        }

        proxyPanel = JPanel().apply {
            add(proxySettingsButton)
            setBounds(150, 205, 120, 40)
        }

        controlPanel = JPanel().apply { layout = null }
        tabFrame = JFrame("").apply {
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
            add(testConnectPanel)
            add(okPanel)
            add(cancelPanel)
        }

        tab2Frame = JFrame("").apply {
            setBounds(100, 100, 580, 300);
            layout = null
            add(proxyDescription)
            add(useProxy)
            add(useHTTP)
            add(proxyPanel)
        }

        bigTabFrame = JTabbedPane().apply {
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            addTab("General", null, tabFrame.contentPane, null);
            setMnemonicAt(0, KeyEvent.VK_1)
            addTab("Proxy settings", null, tab2Frame.contentPane, null);
            setMnemonicAt(1, KeyEvent.VK_2)

        }

        mainFrame = JFrame("YouTrack").apply {
            setBounds(100, 100, 560, 320)
            add(bigTabFrame)
            isVisible = true
        }

    }

}
