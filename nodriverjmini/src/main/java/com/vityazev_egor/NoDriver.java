package com.vityazev_egor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CommandsProcessor;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WebSocketClient;
import com.vityazev_egor.Models.DevToolsInfo;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NoDriver{
    private Process chrome;
    private Thread consoleListener;
    private final CustomLogger logger = new CustomLogger(NoDriver.class.getName());
    private WebSocketClient socketClient;
    private CommandsProcessor cmdProcessor = new CommandsProcessor();

    public static Boolean isInit = false;

    public NoDriver() throws IOException{

        // запускаем браузер и перехватываем вывод в консоли
        ProcessBuilder brower = new ProcessBuilder(
            "google-chrome", 
            "--remote-debugging-port=9222", 
            "--remote-allow-origins=*", 
            "--disable-gpu"
        );
        brower.redirectErrorStream(true);
        chrome = brower.start();
        consoleListener = new Thread(new ConsoleListener(chrome));
        consoleListener.start();
        
        while (!isInit) {
            Shared.sleep(1000);
        }
        logger.info("Chrome inti done");

        findNewTab();
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

    public void loadUrl(String url){
        String json = cmdProcessor.genLoadUrl(url);
        socketClient.sendCommand(json);
    }

    public String getTitle(){
        String json = cmdProcessor.genExecuteJs("document.title");
        socketClient.sendCommandAsync(json);
        String result = socketClient.waitResult(2);
        return cmdProcessor.getJsResult(result);
    }

    public void emulateClick(Integer x, Integer y){
        String[] json = cmdProcessor.genMouseClick(x, y);
        socketClient.sendCommand(json[0]);
        socketClient.sendCommand(json[1]);
    }

    public void enterText(String text, String elementId){
        List<String> jsons = cmdProcessor.genTextInput(text, elementId);
        for (String json : jsons){
            socketClient.sendCommand(json);
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

    public void testElementLocation(String elementId){
        String js = cmdProcessor.getElementLocation.replace("ELEMENT_ID", elementId);
        executeJS(js);
    }
    
    public void exit(){
        logger.warning("Destroying chrome");
        chrome.destroy();
        isInit = false;
    }
}
