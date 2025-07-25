package com.vityazev_egor;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CDPCommandBuilder;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebSocketClient;
import com.vityazev_egor.Core.Driver.Input;
import com.vityazev_egor.Core.Driver.Misc;
import com.vityazev_egor.Core.Driver.Navigation;
import com.vityazev_egor.Core.Driver.XDO;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.WebElements.WebElement;
import com.vityazev_egor.Models.DevToolsInfo;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NoDriver{
    private static final boolean DEBUG_MODE = false;
    
    @Getter
    private Process chrome;
    private final CustomLogger logger = new CustomLogger(NoDriver.class.getName());
    @Getter
    private WebSocketClient socketClient;
    private String tabId;
    public final boolean isWindows;

    // переменные, который используются для корректировки нажатий через xdo
    @Getter
    @Setter
    private Integer calibrateX = 0, calibrateY = 0;

    // расширение функционала
    @Getter
    private final XDO xdo;
    @Getter
    private final Input input;
    @Getter
    private final Navigation navigation;
    @Getter
    private final Misc misc;

    public NoDriver(String socks5Proxy, Boolean enableHeadless) throws IOException{
        this.isWindows = isWindowsOS();
        
        if (isWindows) {
            chrome = launchChromeWindows(socks5Proxy, enableHeadless);
        } else {
            chrome = launchChromeLinux(socks5Proxy, enableHeadless);
        }

        CountDownLatch initLatch = new CountDownLatch(1);
        ConsoleListener consoleListenerInstance = new ConsoleListener(chrome, DEBUG_MODE, initLatch);
        Thread consoleListener = new Thread(consoleListenerInstance);
        consoleListener.start();
        
        try {
            if (!initLatch.await(30, TimeUnit.SECONDS)) {
                throw new IOException("Chrome initialization timeout after 30 seconds");
            }
            
            // Проверяем, была ли инициализация успешной
            if (!consoleListenerInstance.isInitializationSuccessful()) {
                throw new IOException("Chrome process failed to initialize properly");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Chrome initialization was interrupted", e);
        }
        
        logger.info("Chrome initialization done");
        xdo = new XDO(this);
        input = new Input(this);
        navigation = new Navigation(this);
        misc = new Misc(this);
        findNewTab();
    }

    /**
     * Determines if the current operating system is Windows.
     *
     * @return {@code true} if running on Windows, {@code false} otherwise.
     */
    private boolean isWindowsOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win");
    }

    /**
     * Gets the absolute path to the Chrome user data directory.
     * Creates the directory if it doesn't exist.
     *
     * @return Absolute path to the nodriverData directory
     * @throws IOException if the directory cannot be created
     */
    private String getUserDataDir() throws IOException {
        // Get current working directory (where the program is running)
        String currentDir = System.getProperty("user.dir");
        File userDataDir = new File(currentDir, "nodriverData");
        
        // Create directory if it doesn't exist
        if (!userDataDir.exists()) {
            if (!userDataDir.mkdirs()) {
                throw new IOException("Cannot create user data directory: " + userDataDir.getAbsolutePath());
            }
            logger.info("Created Chrome user data directory: " + userDataDir.getAbsolutePath());
        }
        
        return userDataDir.getAbsolutePath();
    }

    /**
     * Launches Chrome browser on Linux using google-chrome command.
     *
     * @param socks5Proxy SOCKS5 proxy configuration (optional)
     * @param enableHeadless whether to run Chrome in headless mode
     * @return Process object representing the Chrome browser process
     * @throws IOException if Chrome cannot be started
     */
    private Process launchChromeLinux(String socks5Proxy, Boolean enableHeadless) throws IOException {
        ProcessBuilder browser = new ProcessBuilder(
            "google-chrome", 
            "--remote-debugging-port=9222", 
            "--remote-allow-origins=*", 
            "--window-size=1280,1060",
            "--no-first-run",
            "--no-default-browser-check",
            "--lang=en",
            "--accept-language=en-US,en",
            "--user-data-dir=" + getUserDataDir()
        );
        
        if (socks5Proxy != null && !socks5Proxy.isEmpty()) {
            browser.command().add("--proxy-server=socks5://" + socks5Proxy);
        }
        
        if (enableHeadless) {
            browser.command().add("--headless");
        }

        if (System.getProperty("user.name").contains("root")) {
            browser.command().add("--no-sandbox");
        }
        
        browser.redirectErrorStream(true);
        return browser.start();
    }

    /**
     * Launches Chrome browser on Windows using chrome.exe from common installation paths.
     *
     * @param socks5Proxy SOCKS5 proxy configuration (optional)
     * @param enableHeadless whether to run Chrome in headless mode
     * @return Process object representing the Chrome browser process
     * @throws IOException if Chrome cannot be started
     */
    private Process launchChromeWindows(String socks5Proxy, Boolean enableHeadless) throws IOException {
        String chromeExecutable = findChromeExecutableWindows();
        
        ProcessBuilder browser = new ProcessBuilder(
            chromeExecutable,
            "--remote-debugging-port=9222", 
            "--remote-allow-origins=*", 
            "--window-size=1280,1060",
            "--no-first-run",
            "--no-default-browser-check",
            "--lang=en",
            "--accept-language=en-US,en",
            "--user-data-dir=" + getUserDataDir()
        );
        
        if (socks5Proxy != null && !socks5Proxy.isEmpty()) {
            browser.command().add("--proxy-server=socks5://" + socks5Proxy);
        }
        
        if (enableHeadless) {
            browser.command().add("--headless");
        }
        
        browser.redirectErrorStream(true);
        return browser.start();
    }

    /**
     * Finds Chrome executable path on Windows by checking common installation directories.
     *
     * @return Path to Chrome executable
     * @throws IOException if Chrome executable cannot be found
     */
    private String findChromeExecutableWindows() throws IOException {
        String[] possiblePaths = {
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"
        };
        
        for (String path : possiblePaths) {
            java.io.File chromeFile = new java.io.File(path);
            if (chromeFile.exists()) {
                logger.info("Found Chrome at: " + path);
                return path;
            }
        }
        
        throw new IOException("Chrome executable not found. Please ensure Chrome is installed.");
    }

    public NoDriver() throws IOException{
        this(null, false);
    }

    public NoDriver(String socks5Proxy) throws IOException{
        this(socks5Proxy, false);
    }

    // find web socket url to control new tab of chrome
    private void findNewTab(){
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder()
            .url("http://localhost:9222/json")
            .get()
            .build();
        ObjectMapper objectMapper = new ObjectMapper();

        RetryConfig retryConfig = new RetryConfigBuilder()
            .retryOnAnyException()
            .withMaxNumberOfTries(30)
            .withDelayBetweenTries(Duration.ofSeconds(2))
            .withFixedBackoff()
            .build();

        Callable<Boolean> connectToChrome = () -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Can't get answer from chrome local server, HTTP status: " + response.code());
                }
                if (response.body() == null) return false;
                String rawJson = response.body().string();
                List<DevToolsInfo> tabsInfo = Arrays.asList(objectMapper.readValue(rawJson, DevToolsInfo[].class));
                tabsInfo.forEach(System.out::println);
                DevToolsInfo newTab = tabsInfo.stream()
                    .filter(t -> t.getUrl().equals("chrome://newtab/"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Can't find new tab!"));
                
                logger.info("Found new tab");
                logger.info(newTab.getWebSocketDebuggerUrl());
                this.tabId = newTab.getId();
                this.socketClient = new WebSocketClient(newTab.getWebSocketDebuggerUrl());
                return true;
            }
        };

        try {
            var executor = new CallExecutorBuilder<Boolean>()
                .config(retryConfig)
                .afterFailedTryListener(status -> 
                    logger.warning("Connection attempt " + status.getTotalTries() + 
                        " failed: " + status.getLastExceptionThatCausedRetry().getMessage())
                )
                .build();
                
            executor.execute(connectToChrome);
        } catch (Exception e) {
            logger.error("Failed to connect to Chrome DevTools after 30 attempts: " + e.getMessage());
            throw new RuntimeException("Unable to establish connection with Chrome DevTools after 60 seconds. Browser may not be ready.", e);
        }
    }

    public Optional<Double> getCurrentPageTime(){
        var sDouble = executeJSAndGetResult("performance.now()");
        return sDouble.map(Double::parseDouble);
    }

    public Optional<String> getHtml(){
        return executeJSAndGetResult("document.documentElement.outerHTML");
    }

    public Optional<String> getTitle(){
        return executeJSAndGetResult("document.title");
    }

    public Optional<String> getCurrentUrl(){
        return executeJSAndGetResult("window.location.href");
    }

    public Optional<Dimension> getViewPortSize() {
        var portWidth = executeJSAndGetResult("window.innerWidth");
        var portHeight = executeJSAndGetResult("window.innerHeight");
        // Если оба результата присутствуют, создаем и возвращаем Optional<Dimension>
        if (portWidth.isPresent() && portHeight.isPresent()) {
            return Optional.of(new Dimension(Integer.parseInt(portWidth.get()), Integer.parseInt(portHeight.get())));
        } else {
            return Optional.empty();
        }
    }    

    public void emulateKey(){
        String keyDown = CDPCommandBuilder.create("Input.dispatchKeyEvent")
            .addParam("type", "keyDown")
            .addParam("key", "End")
            .addParam("code", "End")
            .addParam("keyCode", 35)
            .addParam("modifiers", 0)
            .addParam("autoRepeat", false)
            .addParam("isKeypad", false)
            .addParam("isSystemKey", true)
            .addParam("location", 0)
            .build();

        String keyUp = CDPCommandBuilder.create("Input.dispatchKeyEvent")
            .addParam("type", "keyUp")
            .addParam("key", "End")
            .addParam("code", "End")
            .addParam("keyCode", 35)
            .addParam("modifiers", 0)
            .addParam("autoRepeat", false)
            .addParam("isKeypad", false)
            .addParam("isSystemKey", true)
            .addParam("location", 0)
            .build();

        socketClient.sendCommand(keyDown);
        socketClient.sendCommand(keyUp);
    }

    /**
     * Executes the given JavaScript code in the tab.
     *
     * @param js JavaScript code to execute.
     */
    public void executeJS(String js){
        String command = CDPCommandBuilder.create("Runtime.evaluate")
            .addParam("expression", js)
            .build();
        socketClient.sendAndWaitResult(2, command, 50);
    }

    /**
     * Executes the given JavaScript code in the tab and returns the result.
     * Make sure that your JavaScript code returns only one string!
     *
     * @param js JavaScript code to execute.
     * @return An {@code Optional<String>} containing the result of the JavaScript execution,
     *         or {@code Optional.empty()} if no result is available.
     */
    public Optional<String> executeJSAndGetResult(String js){
        String command = CDPCommandBuilder.create("Runtime.evaluate")
            .addParam("expression", js)
            .build();
        var response = socketClient.sendAndWaitResult(2, command);
        return response.flatMap(CDPCommandBuilder::getJsResult);
    }

    /**
     * Returns single web element in the browser using the specified selector.
     * After that you should make sure that element exists by calling .isExist method
     *
     * @param by The selector used to locate the element.
     * @return A {@code WebElement} representing the found element.
     */
    public WebElement findElement(By by){
        return new WebElement(this, by);
    }

    /**
     * Finds all matching web elements in the tab using the specified selector.
     *
     * @param by The selector used to locate the elements.
     * @return A list of {@code WebElement} objects representing the found elements.
     *         Returns an empty list if no elements are found or if an error occurs.
     */
    public List<WebElement> findElements(By by) {
        var elements = new ArrayList<WebElement>();
        
        // Получаем JavaScript для выбора элементов
        String jsArrayExpression = by.getMultiJavaScript();
        
        // Получаем длину массива элементов
        String lengthJS = jsArrayExpression + ".length";
        var lengthResult = executeJSAndGetResult(lengthJS);
        
        return lengthResult.map(len -> {
            try {
                int arrayLen = Integer.parseInt(len);
                for (int i = 0; i < arrayLen; i++) {
                    String elementJS = String.format("%s[%d]", jsArrayExpression, i);
                    elements.add(new WebElement(this, elementJS));
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse array length", e);
            }
            return elements;
        }).orElse(elements);  // Возвращаем пустой список, если длина не была получена
    }    
    
    public void exit(){
        if (socketClient != null) {
            socketClient.closeSession();
        }
        if (tabId != null) {
            logger.warning("Closing tab with id = " + tabId);
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request request = new Request.Builder()
                    .url("http://localhost:9222/json/close/" + this.tabId)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.body() != null) {
                    logger.warning(response.body().string());
                }
            } catch (Exception ex){
                logger.warning("Failed to close Chrome tab: " + ex.getMessage());
            }
        }
    }
}
