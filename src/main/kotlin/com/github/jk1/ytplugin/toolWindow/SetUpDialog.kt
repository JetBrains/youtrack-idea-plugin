package com.github.jk1.ytplugin.toolWindow

//import com.github.jk1.ytplugin.toolWindow.config.SetupRepositoriesConfigurable
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*


/**
 * Class for the YouTrack toolwindow. Initial for the plugin setup.
 * @author Alina Boshchenko
 */
class SetUpDialog(val project: Project) : ProjectComponent {

    private var myToolWindowContent: JPanel = JPanel(BorderLayout())

    private fun createPanel(): JComponent {
        val labelContainer = JPanel()
        val messageLabel = JLabel("No YouTrack server found")
        val configureLabel = createLink("Configure") {
            SetupWindow(project)
//            SetupRepositoriesConfigurable(project)
        }

        messageLabel.alignmentX = Component.CENTER_ALIGNMENT
        configureLabel.alignmentX = Component.CENTER_ALIGNMENT
        labelContainer.add(messageLabel)
        labelContainer.add(configureLabel)
        myToolWindowContent.add(labelContainer, BorderLayout.NORTH)
        return myToolWindowContent
    }

    private fun createLink(text: String, onClick: () -> Unit): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        val label = SimpleColoredComponent()
        label.append(text, SimpleTextAttributes.LINK_ATTRIBUTES)
        label.cursor = Cursor(Cursor.HAND_CURSOR)
        label.setPreferredSize(Dimension(100, 100))
        dialogPanel.add(label, BorderLayout.CENTER)
        label.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onClick.invoke()
            }
        })
        return label
    }

    val content: JPanel?
        get() = myToolWindowContent

    init {
        createPanel()
    }
}



