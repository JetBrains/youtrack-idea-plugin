package com.github.jk1.ytplugin;

import java.util.List;

public interface YouTrackCommandExecutionResult {

    public boolean isSuccessful();

    public List<String> getExecutionMessages();

    public List<String> getExecutionErrors();
}
