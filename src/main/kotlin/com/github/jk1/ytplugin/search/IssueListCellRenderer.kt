package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class IssueListCellRenderer() : JPanel(BorderLayout()), ListCellRenderer<Any> {

    private val myPriority = JLabel()
    private val myStatus = JLabel()
    private val myId = SimpleColoredComponent()
    private val mySummary = JLabel()
    private val myDescription = JTextArea()
    private val myTime = JLabel()
    private val ID = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListForeground(false))
    private val ID_SELECTED = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListForeground(true))
    private val ID_FIXED = SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, UIUtil.getListForeground(false))
    private val ID_FIXED_SELECTED = SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, UIUtil.getListForeground(true))

    init {
        myId.isOpaque = false
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        //1. Left small line
        val left = createNonOpaquePanel()
        val bottomStub = JLabel(" ")
        val fontName = if (SystemInfo.isMac) "Lucida Grande" else if (SystemInfo.isWindows) "Arial" else "Verdana"
        val font = Font(fontName, Font.PLAIN, 10)
        myPriority.font = Font(fontName, Font.BOLD, 12)
        myPriority.border = EmptyBorder(2, 6, 2, 2)
        bottomStub.font = font
        left.add(myPriority, BorderLayout.NORTH)
        val p = JPanel()
        p.isOpaque = false
        p.add(myStatus)
        myStatus.minimumSize = Dimension(16, 16)
        left.add(p, BorderLayout.CENTER)
        left.add(bottomStub, BorderLayout.SOUTH)
        add(left, BorderLayout.WEST)

        //2. Central area contains other components
        val central = createNonOpaquePanel()
        add(central, BorderLayout.CENTER)

        //3. Create header
        val top = createNonOpaquePanel()
        myId.font = Font(fontName, Font.PLAIN, 12)
        top.add(myId, BorderLayout.CENTER)
        top.add(myTime, BorderLayout.EAST)
        myTime.font = Font(fontName, Font.PLAIN, 10)
        central.add(top, BorderLayout.NORTH)

        //4. Message body
        val main = createNonOpaquePanel()
        val text = createNonOpaquePanel()
        mySummary.font = Font(fontName, Font.BOLD, 12)
        myDescription.font = Font(fontName, Font.PLAIN, 12)
        myDescription.autoscrolls = false

        myDescription.isEditable = false
        myDescription.lineWrap = true
        myDescription.isOpaque = false
        myDescription.wrapStyleWord = true
        main.add(text, BorderLayout.CENTER)
        text.add(mySummary, BorderLayout.NORTH)
        text.add(myDescription, BorderLayout.CENTER)
        central.add(main, BorderLayout.CENTER)
    }

    private fun createNonOpaquePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        return panel
    }

    override fun getListCellRendererComponent(list: JList<out Any>?,
                                              value: Any?, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component? {

        init(value as Issue, isSelected, hasFocus())
        return this
    }

    private fun init(issue: Issue, selected: Boolean, focused: Boolean) {
        background = UIUtil.getListBackground(selected)
        myId.clear()
        myId.append(issue.id)
        myId.isIconOnTheRight = true
        //myTime.text = issue.getPresentableTime()
        myTime.foreground = if (selected) UIUtil.getListForeground(true) else JBColor(Color(75, 107, 244), Color(87, 120, 173))
        //myPriority.icon = if (issue.isMajor()) MyyIcons.IMPORTANT else EmptyIcon.ICON_16
        mySummary.text = "<html>" + issue.summary + "</html>"
        myDescription.text = StringUtil.unescapeXml(issue.description)
        myDescription.foreground = if (selected) UIUtil.getListForeground(selected) else JBColor(Gray._130, Gray._110)
        mySummary.foreground = if (selected) UIUtil.getListForeground(selected) else JBColor(Color(0, 68, 105), Color(160, 110, 0))



        myStatus.icon = object : Icon {
            override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
                GraphicsUtil.setupAAPainting(g)
                if (selected) {
                    g.color = UIUtil.getListForeground(selected)
                } else {
                    (g as Graphics2D).paint = GradientPaint((x + (c.width - 10) / 2).toFloat(), (y + (c.height - 10) / 2).toFloat(), Color(125, 172, 222), x + (c.width - 10) / 2 + 10.toFloat(), y + (c.height - 10) / 2 + 10.toFloat(), Color(69, 86, 182))
                }
                g.fillOval(x + (c.width - 10) / 2, y + (c.height - 10) / 2, 10, 10)
            }

            override fun getIconWidth(): Int {
                return 14
            }

            override fun getIconHeight(): Int {
                return 14
            }

        }
    }
}