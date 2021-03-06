package com.github.jk1.ytplugin.workflowsDebugConfiguration;

import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;

public interface JSRunProfileWithCompileBeforeLaunchOption extends RunProfileWithCompileBeforeLaunchOption {
    @Override
    default boolean isBuildBeforeLaunchAddedByDefault() {
        return false;
    }
}
