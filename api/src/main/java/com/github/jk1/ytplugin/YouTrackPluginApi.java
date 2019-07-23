package com.github.jk1.ytplugin;


import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * YouTrack integration plugin API to be used by other plugins
 */
public interface YouTrackPluginApi {

    /**
     * Open issue preview in YouTrack tool window.
     * This method shouldn't be called from EDT as it may include networking.
     *
     * @param issueId logical issue id to open, e.g. TW-1234
     * @throws IllegalArgumentException if target issue is nowhere to be found
     */
    void openIssueInToolWidow(@NotNull String issueId);

    @NotNull
    List<YouTrackIssue> search(@NotNull String query);

    @NotNull
    YouTrackCommandExecutionResult executeCommand(YouTrackIssue issue, String command);
}