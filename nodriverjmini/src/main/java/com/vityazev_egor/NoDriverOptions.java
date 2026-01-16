package com.vityazev_egor;

public class NoDriverOptions {
    private final int windowWidth;
    private final int windowHeight;
    private final String socks5Proxy;
    private final boolean headless;
    private final boolean fullScreen;

    private NoDriverOptions(Builder builder) {
        this.windowWidth = builder.windowWidth;
        this.windowHeight = builder.windowHeight;
        this.socks5Proxy = builder.socks5Proxy;
        this.headless = builder.headless;
        this.fullScreen = builder.fullScreen;
    }

    /**
     * Creates a new builder with default option values.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public String getSocks5Proxy() {
        return socks5Proxy;
    }

    public boolean isHeadless() {
        return headless;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public static class Builder {
        private int windowWidth = 1280;
        private int windowHeight = 1060;
        private String socks5Proxy = null;
        private boolean headless = false;
        private boolean fullScreen = false;

        /**
         * Sets the browser window width in pixels.
         *
         * @param windowWidth Window width in pixels.
         * @return this builder
         */
        public Builder setWindowWidth(int windowWidth) {
            this.windowWidth = windowWidth;
            return this;
        }

        /**
         * Sets the browser window height in pixels.
         *
         * @param windowHeight Window height in pixels.
         * @return this builder
         */
        public Builder setWindowHeight(int windowHeight) {
            this.windowHeight = windowHeight;
            return this;
        }

        /**
         * Sets the SOCKS5 proxy host and port in the format "host:port".
         *
         * @param socks5Proxy Proxy configuration string.
         * @return this builder
         */
        public Builder setSocks5Proxy(String socks5Proxy) {
            this.socks5Proxy = socks5Proxy;
            return this;
        }

        /**
         * Enables or disables headless mode.
         *
         * @param headless Whether to run Chrome in headless mode.
         * @return this builder
         */
        public Builder setHeadless(boolean headless) {
            this.headless = headless;
            return this;
        }

        /**
         * Enables or disables fullscreen mode.
         *
         * @param fullScreen Whether to start Chrome in fullscreen mode.
         * @return this builder
         */
        public Builder setFullScreen(boolean fullScreen) {
            this.fullScreen = fullScreen;
            return this;
        }

        /**
         * Builds an immutable {@link NoDriverOptions} instance.
         *
         * @return configured options
         */
        public NoDriverOptions build() {
            return new NoDriverOptions(this);
        }
    }
}
