package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.CustomAttributesHandler
import com.intellij.openapi.application.ApplicationManager
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.HttpHostConnectException
import java.util.concurrent.Callable

class CustomAttributesClient (override val repository: YouTrackServer) : RestClientTrait, ResponseLoggerTrait {

    fun getCustomAttributesForProjectInCallable(projectId: String): Map<String, List<String>>{
        val future = ApplicationManager.getApplication().executeOnPooledThread (
            Callable {
                checkIfProjectHasCustomAttributes(projectId)
            })
        return future.get() ?: mapOf()
    }

    private fun checkIfProjectHasCustomAttributes(projectId: String): Map<String, List<String>> {
        val builder = URIBuilder("${repository.url}/api/admin/projects/$projectId/timeTrackingSettings/attributes")

        builder.setParameter("fields", "id,name,values(id,name)")
        val method = HttpGet(builder.build())

        return try {
            method.execute(handleErrorStatusCode = false) {
                CustomAttributesHandler().parseCustomAttributes(it.asJsonArray)
            }
        } catch (e: HttpHostConnectException){
            logger.debug("Error in checkIfTrackingIsEnabled: ${e.message}")
            mapOf()
        } catch (e: RuntimeException){
            logger.debug("No custom work items attributes found: ${e.message}")
            mapOf()
        }
    }
}