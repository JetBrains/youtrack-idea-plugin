package com.github.jk1.ytplugin.timeTracker

import javax.swing.JOptionPane


object TrackerNotifier{
    fun infoBox(infoMessage: String?, titleBar: String) {
        JOptionPane.showMessageDialog(null, infoMessage, "Time tracking $titleBar", JOptionPane.INFORMATION_MESSAGE)
    }
}
