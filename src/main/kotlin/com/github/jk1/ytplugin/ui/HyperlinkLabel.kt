package com.github.jk1.ytplugin.ui

import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.SwingConstants

class HyperlinkLabel(anchor: String, url: String) : JLabel() {

    private val clickListener = object: MouseAdapter(){
        override fun mouseClicked(e: MouseEvent) {
            BrowserLauncher.getInstance().open(url)
        }
    }

    init {
        text = "<html><a href='$url'>$anchor</a></html>"
        horizontalAlignment = SwingConstants.LEFT
        isOpaque = false
        toolTipText = url
        cursor = Cursor(Cursor.HAND_CURSOR)
        if (UIUtil.isUnderDarcula()) {
            foreground = Color(87, 120, 173)
        }
        addMouseListener(clickListener)
    }
}