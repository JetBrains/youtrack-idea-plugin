package com.github.jk1.ytplugin.components


open class YouTrackPluginException(message : String) : Exception(message) {
}

class YouTrackRepositoryNotConfiguredException(message : String) : YouTrackPluginException(message) {
}

class NoYouTrackRepositoryException(message : String) : YouTrackPluginException(message) {
}

class TaskManagementDisabledException(message : String) : YouTrackPluginException(message) {
}