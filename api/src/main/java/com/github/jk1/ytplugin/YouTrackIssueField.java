package com.github.jk1.ytplugin;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface YouTrackIssueField {

    @NotNull
    String getFieldName();

    @NotNull
    List<String> getFieldValues();
}
