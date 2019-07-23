package com.github.jk1.ytplugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface YouTrackIssue {

    @NotNull
    String getIssueId();

    @NotNull
    String getIssueSummary();

    @Nullable
    String getIssueDescription();

    @NotNull
    List<YouTrackIssueField> getIssueFields();
}
