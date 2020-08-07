package com.github.jk1.ytplugin.setup.config

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor


class TaskSearchableOptionContributor : SearchableOptionContributor() {
    val ID = "tasks.servers"
    override fun processOptions(processor: SearchableOptionProcessor) {
        processor.addOptions("YouTrack", null, null, ID, null, true)
    }
}