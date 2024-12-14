package com.vityazev_egor;

import java.awt.Dimension;
import java.awt.Point;
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
    @Getter
    private CommandsProcessor cmdProcessor = new CommandsProcessor();

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

    public NoDriver(String socks5Proxy) throws IOException{
        // запускаем браузер и перехватываем вывод в консоли
        ProcessBuilder browser = new ProcessBuilder(
            "google-chrome", 
            "--remote-debugging-port=9222", 
            "--remote-allow-origins=*", 
            //"--disable-gpu",
            "--window-size=1280,1060",
            "--no-first-run",
            "--no-default-browser-check",
            "--lang=en",
            "--accept-language=en-US,en",
            "--user-data-dir=nodriverData"
        );
        if (socks5Proxy != null && !socks5Proxy.isEmpty()) {
            // Добавляем аргумент для прокси без кавычек вокруг URL
            browser.command().add("--proxy-server=socks5://" + socks5Proxy);
        }
        browser.redirectErrorStream(true);
        chrome = browser.start();
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

        findNewTab();
    }

    public NoDriver() throws IOException{
        this(null);
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
            if (response.isSuccessful()){
                String rawJson = response.body().string();
                List<DevToolsInfo> tabsInfo = Arrays.asList(objectMapper.readValue(rawJson, DevToolsInfo[].class));
                DevToolsInfo newTab = tabsInfo.stream().filter(t->t.getUrl().equals("chrome://newtab/")).findFirst().orElse(null);
                if (newTab != null){
                    logger.info("Found new tab");
                    logger.info(newTab.getWebSocketDebuggerUrl());
                    this.socketClient = new WebSocketClient(newTab.getWebSocketDebuggerUrl());
                }
                else{
                    throw new IOException("Can't find new tab");
                }
            }

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

    public void executeJS(String js){
        String json = cmdProcessor.genExecuteJs(js);
        socketClient.sendCommand(json);
    }

    public Optional<String> executeJSAndGetResult(String js){
        String json = cmdProcessor.genExecuteJs(js);
        //System.out.println(json);
        var response = socketClient.sendAndWaitResult(2, json);
        if (response.isPresent()){
            return cmdProcessor.getJsResult(response.get());
        }
        else{
            return Optional.empty();
        }
    }

    public WebElement findElement(By by){
        return new WebElement(this, by);
    }

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
        logger.warning("Closing chrome");
        socketClient.sendCommand(cmdProcessor.genCloseBrowser());
        try {
            chrome.waitFor();
        } catch (Exception e) {}
        isInit = false;
    }
}
