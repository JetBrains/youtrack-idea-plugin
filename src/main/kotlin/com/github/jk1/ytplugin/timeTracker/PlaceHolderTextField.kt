package com.github.jk1.ytplugin.timeTracker

import com.intellij.ui.components.JBTextField
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints


class PlaceholderTextField(pText: String?) : JBTextField(pText) {
    var placeholder: String? = null

    override fun paintComponent(pG: Graphics) {
        super.paintComponent(pG)
        if (placeholder == null || placeholder!!.length == 0 || text.isNotEmpty()) {
            return
        }
        val g = pG as Graphics2D
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = disabledTextColor
        g.drawString(placeholder, insets.left + 5, pG.getFontMetrics()
                .maxAscent + insets.top + 3)
    }
}