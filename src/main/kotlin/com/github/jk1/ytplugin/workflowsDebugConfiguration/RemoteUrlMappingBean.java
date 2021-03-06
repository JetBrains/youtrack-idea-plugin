package com.github.jk1.ytplugin.workflowsDebugConfiguration;

import com.intellij.util.UriUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

public class RemoteUrlMappingBean {
    @Attribute("local-file")
    public String localFilePath;

    private String remoteUrl;

    @SuppressWarnings("UnusedDeclaration")
    public RemoteUrlMappingBean() {
    }

    public RemoteUrlMappingBean(String localFilePath, String remoteUrl) {
        this.localFilePath = localFilePath;
        setRemoteUrl(remoteUrl);
    }

    @Attribute("url")
    public String getRemoteUrl() {
        return remoteUrl;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRemoteUrl(String value) {
        // ability to read incorrect (old) data from iml (1) from old idea version 2) file was edited manually)
        remoteUrl = value == null ? null : UriUtil.trimTrailingSlashes(value);
    }
}