package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.tasks.YouTrackServer
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder


class UserRestClient(override val repository: YouTrackServer) : RestClientTrait {

    fun getWorkItemsForUser(): List<IssueWorkItem> {
        val builder = URIBuilder("${repository.url}/api/workItems")
        builder.addParameter("author", "me")
                .addParameter("fields", "text,issue(idReadable),type(name),created," +
                        "duration(presentation,minutes),author(name),creator(name),date,id,attributes(name,id,value(name))")
                .addParameter("sort", "descending")

        val method = HttpGet(builder.build())
        val items = method.execute { element ->
            element.asJsonArray.mapNotNull { IssueJsonParser.parseWorkItem(it) }
        }
        return items.sortedWith(compareByDescending { it.date })
    }

}