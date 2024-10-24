package com.vityazev_egor.Models;

import lombok.Data;

@Data
public class DevToolsInfo {
    private String description;
    private String devtoolsFrontendUrl;
    private String id;
    private String parentId;
    private String title;
    private String type;
    private String url;
    private String webSocketDebuggerUrl;
}
