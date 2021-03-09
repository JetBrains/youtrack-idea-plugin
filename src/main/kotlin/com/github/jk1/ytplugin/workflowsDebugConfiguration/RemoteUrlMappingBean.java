package com.intellij.javascript.debugger.execution;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.intellij.util.UriUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

@Tag("mapping")
public class RemoteUrlMappingBean{
    @Attribute("local-file")
    public String localFilePath;
    private String remoteUrl;

    public RemoteUrlMappingBean() {
    }

    public RemoteUrlMappingBean(String localFilePath, String remoteUrl) {
        this.localFilePath = localFilePath;
        this.setRemoteUrl(remoteUrl);
    }

    @Attribute("url")
    public String getRemoteUrl() {
        return this.remoteUrl;
    }

    public void setRemoteUrl(String value) {
        this.remoteUrl = value == null ? null : UriUtil.trimTrailingSlashes(value);
    }
}
