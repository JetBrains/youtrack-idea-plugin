package com.github.jk1.ytplugin.ui

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.components.ServiceManager
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.SwingConstants

class HyperlinkLabel(anchor: String, url: String, icon: Icon? = null) : JLabel() {

    private val clickListener = object: MouseAdapter(){
        override fun mouseClicked(e: MouseEvent) {
            ServiceManager.getService(BrowserLauncher::class.java).open(url)
        }
    }

    init {
        this.icon = icon
        this.text = "<html><a href='$url'>$anchor</a></html>"
        this.horizontalAlignment = SwingConstants.LEFT
        this.isOpaque = false
        this.toolTipText = url
        this.cursor = Cursor(Cursor.HAND_CURSOR)
        if (UIUtil.isUnderDarcula()) {
            this.foreground = Color(87, 120, 173)
        }
        addMouseListener(clickListener)
    }
}