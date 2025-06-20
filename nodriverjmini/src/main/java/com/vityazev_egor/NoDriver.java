package com.vityazev_egor;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CommandsProcessor;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
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
    @Getter
    private Process chrome;
    private Thread consoleListener;
    private final CustomLogger logger = new CustomLogger(NoDriver.class.getName());
    @Getter
    private WebSocketClient socketClient;
    private String tabId;
    @Getter
    private CommandsProcessor cmdProcessor = new CommandsProcessor();
    private final boolean isWindows;

    public static Boolean isInit = false;

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
        
        consoleListener = new Thread(new ConsoleListener(chrome));
        consoleListener.start();
        
        while (!isInit) {
            Shared.sleep(1000);
        }
        logger.info("Chrome inti done");

        // иницилизируем классы для расширения функционала
        xdo = new XDO(this);
        input = new Input(this);
        navigation = new Navigation(this);
        misc = new Misc(this);

        Shared.sleep(2000);
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
            "--user-data-dir=nodriverData"
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
            "--user-data-dir=nodriverData"
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
        
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()){
                throw new IOException("Can't get answer from chrome local server");
            }
            String rawJson = response.body().string();
            List<DevToolsInfo> tabsInfo = Arrays.asList(objectMapper.readValue(rawJson, DevToolsInfo[].class));
            tabsInfo.forEach(System.out::println);
            DevToolsInfo newTab = tabsInfo.stream().filter(t->t.getUrl().equals("chrome://newtab/")).findFirst().orElseThrow(() -> new IOException("Can't find new tab!"));
            logger.info("Found new tab");
            logger.info(newTab.getWebSocketDebuggerUrl());
            this.tabId = newTab.getId();
            this.socketClient = new WebSocketClient(newTab.getWebSocketDebuggerUrl());
        } catch (IOException e) {
            e.printStackTrace();
            exit();
        }
        catch (Exception e){
            e.printStackTrace();
            exit();
        }
    }

    public Optional<Double> getCurrentPageTime(){
        var sDouble = executeJSAndGetResult("performance.now()");
        if (sDouble.isPresent()){
            return Optional.of(Double.parseDouble(sDouble.get()));
        }
        else{
            return Optional.empty();
        }
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
        String[] jsons = cmdProcessor.genKeyInput();
        socketClient.sendCommand(jsons[0]);
        socketClient.sendCommand(jsons[1]);
    }

    /**
     * Executes the given JavaScript code in the tab.
     *
     * @param js JavaScript code to execute.
     */
    public void executeJS(String js){
        String json = cmdProcessor.genExecuteJs(js);
        socketClient.sendAndWaitResult(2, json, 50);
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
        String json = cmdProcessor.genExecuteJs(js);
        //System.out.println(json);
        var response = socketClient.sendAndWaitResult(2, json);
        return response.map(r -> cmdProcessor.getJsResult(r)).orElse(Optional.empty());
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
        socketClient.closeSession();
        if (tabId != null) {
            logger.warning("Closing tab with id = " + tabId);
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request request = new Request.Builder()
                    .url("http://localhost:9222/json/close/" + this.tabId)
                    .get()
                    .build();
            try {
                var response = client.newCall(request).execute();
                logger.warning(response.body().string());
            } catch (Exception ex){}
        }
        isInit = false;
    }
}
