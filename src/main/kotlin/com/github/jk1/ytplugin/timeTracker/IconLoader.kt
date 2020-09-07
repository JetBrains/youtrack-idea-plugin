package com.github.jk1.ytplugin.timeTracker

import javax.swing.Icon
import javax.swing.ImageIcon

class IconLoader {
    companion object {
        fun loadIcon(path: String) : Icon {
            return ImageIcon(this::class.java.classLoader.getResource(path))
        }
    }
}