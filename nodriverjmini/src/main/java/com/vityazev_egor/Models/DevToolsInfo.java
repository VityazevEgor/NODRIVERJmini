package com.vityazev_egor.Models;

// TODO i can delete this and mybe use less code to extract ws link
public class DevToolsInfo {
    private String description;
    private String devtoolsFrontendUrl;
    private String id;
    private String parentId;
    private String title;
    private String type;
    private String url;
    private String webSocketDebuggerUrl;


    // Getters and setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDevtoolsFrontendUrl() {
        return devtoolsFrontendUrl;
    }

    public void setDevtoolsFrontendUrl(String devtoolsFrontendUrl) {
        this.devtoolsFrontendUrl = devtoolsFrontendUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWebSocketDebuggerUrl() {
        return webSocketDebuggerUrl;
    }

    public void setWebSocketDebuggerUrl(String webSocketDebuggerUrl) {
        this.webSocketDebuggerUrl = webSocketDebuggerUrl;
    }
}
