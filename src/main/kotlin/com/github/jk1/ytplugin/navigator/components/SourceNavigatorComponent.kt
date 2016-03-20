package com.github.jk1.ytplugin.navigator.components

import com.github.jk1.ytplugin.components.ComponentAware
import com.github.jk1.ytplugin.navigator.SourceNavigatorServer
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project


class SourceNavigatorComponent(override val project: Project) : AbstractProjectComponent(project), ComponentAware {

    val server = SourceNavigatorServer(project)

    override fun projectOpened() {
        server.start()
    }

    override fun projectClosed() {
        server.stop()
    }
}