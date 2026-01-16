package com.vityazev_egor;

public class NoDriverOptions {
    private int windowWidth = 1280;
    private int windowHeight = 1060;
    private String socks5Proxy = null;
    private boolean headless = false;
    private boolean fullScreen = false;

    public int getWindowWidth() {
        return windowWidth;
    }

    public NoDriverOptions setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
        return this;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public NoDriverOptions setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
        return this;
    }

    public String getSocks5Proxy() {
        return socks5Proxy;
    }

    public NoDriverOptions setSocks5Proxy(String socks5Proxy) {
        this.socks5Proxy = socks5Proxy;
        return this;
    }

    public boolean isHeadless() {
        return headless;
    }

    public NoDriverOptions setHeadless(boolean headless) {
        this.headless = headless;
        return this;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public NoDriverOptions setFullScreen(boolean fullScreen) {
        this.fullScreen = fullScreen;
        return this;
    }
}
