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


    /**
     * Search for issues in Youtrack.
     * This method makes synchronous network calls and shouldn't be called on EDT.
     *
     * @param query https://www.jetbrains.com/help/youtrack/incloud/Search-and-Command-Attributes.html
     * @return list of issues, maybe empty
     */
    @NotNull
    List<YouTrackIssue> search(@NotNull String query);

    /**
     * Tries to execute a command against an issue in YouTrack.
     * This method makes synchronous network calls and shouldn't be called on EDT.
     *
     * @param issue an issue to apply command to
     * @param command https://www.jetbrains.com/help/youtrack/incloud/Command-Reference.html
     * @return command execution result with all errors occurred in process
     */
    @NotNull
    YouTrackCommandExecutionResult executeCommand(YouTrackIssue issue, String command);
}