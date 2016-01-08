package com.github.jk1.ytplugin


open class YouTrackPluginException(message: String) : Exception(message) {}

class TaskManagementDisabledException() :
        YouTrackPluginException("Task Management plugin is disabled") {}

class NoYouTrackRepositoryException() :
        YouTrackPluginException("No YouTrack server found") {}

class YouTrackRepositoryNotConfiguredException() :
        YouTrackPluginException("YouTrack server integration is not configured yet") {}

class NoActiveYouTrackTaskFoundException() :
        YouTrackPluginException("No YouTrack issue selected as an active task") {}