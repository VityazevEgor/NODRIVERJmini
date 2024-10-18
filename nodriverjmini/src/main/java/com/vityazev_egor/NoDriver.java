package com.vityazev_egor;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.apache.commons.imaging.Imaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CommandsProcessor;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WebSocketClient;
import com.vityazev_egor.Core.Driver.Input;
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

    public NoDriver(String socks5Proxy) throws IOException{
        // запускаем браузер и перехватываем вывод в консоли
        ProcessBuilder browser = new ProcessBuilder(
            "google-chrome", 
            "--remote-debugging-port=9222", 
            "--remote-allow-origins=*", 
            "--disable-gpu",
            "--window-size=1280,1060",
            "--no-first-run",
            "--no-default-browser-check",
            "--lang=en",
            "--accept-language=en-US,en",
            "--user-data-dir=nodriverData"
        );
        if (socks5Proxy != null) {
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

    public Optional<Point> getElementPosition(WebElement element){
        return element.getPosition(this);
    }
    public Optional<Dimension> getElementSize(WebElement element){
        return element.getSize(this);
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
        // Генерация JSON для выполнения JavaScript и получения ширины и высоты окна
        String jsonWidth = cmdProcessor.genExecuteJs("window.innerWidth");
        String jsonHeight = cmdProcessor.genExecuteJs("window.innerHeight");
    
        // Отправка команды и ожидание результата для ширины
        var responseWidth = socketClient.sendAndWaitResult(2, jsonWidth);
        String widthResult = responseWidth.isPresent() ? cmdProcessor.getJsResult(responseWidth.get()) : null;
    
        // Отправка команды и ожидание результата для высоты
        var responseHeight = socketClient.sendAndWaitResult(2, jsonHeight);
        String heightResult = responseHeight.isPresent() ? cmdProcessor.getJsResult(responseHeight.get()) : null;
    
        // Если оба результата присутствуют, создаем и возвращаем Optional<Dimension>
        if (widthResult != null && heightResult != null) {
            return Optional.of(new Dimension(Integer.parseInt(widthResult), Integer.parseInt(heightResult)));
        } else {
            return Optional.empty();
        }
    }    

    public void emulateKey(){
        String[] jsons = cmdProcessor.genKeyInput();
        socketClient.sendCommand(jsons[0]);
        socketClient.sendCommand(jsons[1]);
    }

    public Optional<BufferedImage> captureScreenshot(Path screenSavePath){
        var response = socketClient.sendAndWaitResult(2, cmdProcessor.genCaptureScreenshot());
        if (!response.isPresent()) return Optional.empty();
        var baseData = cmdProcessor.getScreenshotData(response.get());
        if (!baseData.isPresent()) return Optional.empty();
        
        byte[] imageBytes = Base64.getDecoder().decode(baseData.get());
        try {
            if (screenSavePath != null) Files.write(screenSavePath, imageBytes);
            // вот тут я преобразую байты картинки в формате PNG
            return Optional.of( Imaging.getBufferedImage(imageBytes));
        } catch (Exception e) {
            logger.warning("Can't convert bytes to BufferedImage");
            return Optional.empty();
        }
    }

    public Optional<BufferedImage> captureScreenshot(){
        return captureScreenshot(null);
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
            return Optional.ofNullable(cmdProcessor.getJsResult(response.get()));
        }
        else{
            return Optional.empty();
        }
    }

    public void clearCookies(){
        socketClient.sendCommand(cmdProcessor.genClearCookies());
    }

    public WebElement findElement(By by){
        return new WebElement(by);
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
                    elements.add(new WebElement(elementJS));
                }
            } catch (NumberFormatException e) {
                logger.error("Failed to parse array length", e);
            }
            return elements;
        }).orElse(elements);  // Возвращаем пустой список, если длина не была получена
    }    
    
    public void exit(){
        logger.warning("Destroying chrome");
        chrome.destroy();
        isInit = false;
    }
}
