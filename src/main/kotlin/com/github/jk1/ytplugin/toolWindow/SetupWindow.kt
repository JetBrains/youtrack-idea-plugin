package com.github.jk1.ytplugin.toolWindow

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import javax.swing.*

/**
 * Class for window for initial Setup of YouTrack
 * @author Akina Boshchenko
 */
class SetupWindow(val project: Project) : ProjectComponent {

    private lateinit var mainFrame: JFrame
    private lateinit var serverUrl: JLabel
    private lateinit var tokenField: JLabel
    private lateinit var getTokenField: JLabel
    private lateinit var advertiserField: JLabel
    private lateinit var controlPanel: JPanel
    private lateinit var shareUrl: JCheckBox
    private lateinit var useProxy: JCheckBox
    private lateinit var useHTTP: JCheckBox
    private lateinit var loginAnon: JCheckBox
    private lateinit var testConnectPanel: JPanel
    private lateinit var proxyPanel: JPanel

    private var testConnectButton = JButton("Test connection")
    private var proxySettingsButton = JButton("Proxy settings...")
    private var inputUrl = JTextArea("")
    private var inputToken = JPasswordField("")


    init {
        prepareDialogWindow()
    }

    fun getAdvertiser(): String? {
        return "<html>Not YouTrack customer yet? Get <a href='https://www.jetbrains.com/youtrack/download/get_youtrack.html?idea_integration'>YouTrack</a></html>"
    }

    fun getTokenHelp(): String? {
        return "<html><a href='https://www.jetbrains.com/help/youtrack/incloud/Manage-Permanent-Token.html'>Get token</a></html>"
    }

    private fun prepareDialogWindow() {
        serverUrl = JLabel("Server Url:")
        serverUrl.setBounds(65, 60, 100, 17);
        inputUrl.setBounds(150, 60, 375, 17)

        tokenField = JLabel("Permanent token:")
        tokenField.setBounds(15, 120, 150, 17)
        inputToken.apply {
            setEchoChar('\u25CF')
            setBounds(150, 120, 380, 25)
        }


        val myAdvertiser = getAdvertiser()
        advertiserField = JLabel(myAdvertiser)
        advertiserField.setBounds(240, 30, 300, 17)

        getTokenField = JLabel(getTokenHelp())
        getTokenField.setBounds(150, 150, 100, 17)

        shareUrl = JCheckBox("Share Url", true)
        shareUrl.setBounds(400, 90, 100, 17)

        loginAnon = JCheckBox("Login Anonymously", false)
        loginAnon.setBounds(150, 90, 170, 17)

        useHTTP = JCheckBox("Use HTTP", false)
        useHTTP.setBounds(440, 180, 100, 17);

        useProxy = JCheckBox("Use Proxy", false)
        useProxy.setBounds(300, 180, 100, 17)

        proxySettingsButton.apply {
            actionCommand = "Proxy"
            addActionListener(ButtonClickListener())
            setPreferredSize(Dimension(140, 40))
        }

        testConnectButton.apply {
            setPreferredSize(Dimension(150, 40))
        }

        testConnectButton.addActionListener(ActionListener {

            val setup = SetupTask()

            val myRepository = YouTrackRepository()
            myRepository.url = inputUrl.getText()
            myRepository.password = inputToken.getText()
            myRepository.username = "random"

            setup.testConnection(myRepository, project)
        })

        loginAnon.addItemListener { e ->
            val sel: Int = e.stateChange
            if (sel == ItemEvent.SELECTED) {
                System.out.println("login")
            }
        }

        testConnectPanel = JPanel().apply {
            add(testConnectButton)
            setBounds(400, 200, 200, 40)
        }

        proxyPanel = JPanel().apply {
            add(proxySettingsButton)
            setBounds(150, 165, 120, 40)
        }

        controlPanel = JPanel().apply { layout = null }
        mainFrame = JFrame("General").apply {
            setBounds(100, 100, 580, 280);
            layout = null
            add(shareUrl)
            add(advertiserField)
            add(loginAnon)
            add(serverUrl)
            add(inputUrl)
            add(tokenField)
            add(inputToken)
            add(useProxy)
            add(useHTTP)
            add(getTokenField)
            add(testConnectPanel)
            add(proxyPanel)
            isVisible = true
        }
    }

    private inner class ButtonClickListener : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand == "Proxy") {
                System.out.println(inputUrl.text)
            }
//            myField.text = when (e.actionCommand) {
//                "Proxy" -> "Turn on Proxy settings."
//                "Test" -> "Test connection."
//                else -> "Cancel Button clicked."
//            }
        }
    }
}
