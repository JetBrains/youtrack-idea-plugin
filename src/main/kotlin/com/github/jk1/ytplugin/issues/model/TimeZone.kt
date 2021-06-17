package com.github.jk1.ytplugin.issues.model


import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.intellij.ide.util.PropertyName
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class TimeZone(override val project: Project) : ComponentAware {

    @PropertyName("timeZone.zone")
    var zone: String = AdminRestClient(ComponentAware.of(project).taskManagerComponent
        .getAllConfiguredYouTrackRepositories().firstOrNull()!!).getTimeZone()

}
