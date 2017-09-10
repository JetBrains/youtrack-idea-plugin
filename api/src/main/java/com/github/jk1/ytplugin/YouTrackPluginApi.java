package com.github.jk1.ytplugin;


import org.jetbrains.annotations.NotNull;

/**
 * YouTrack integration plugin API to be used by other plugins
 */
interface YouTrackPluginApi {

    /**
     * Open issue preview in YouTrack tool window.
     * This method shouldn't be called from EDT as it may include networking.
     *
     * @param issueId logical issue id to open, e.g. TW-1234
     * @throws IllegalArgumentException if target issue is nowhere to be found
     */
    void openIssueInToolWidow(@NotNull String issueId);
}