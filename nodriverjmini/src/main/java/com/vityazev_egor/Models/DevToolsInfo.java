package com.vityazev_egor.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
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
