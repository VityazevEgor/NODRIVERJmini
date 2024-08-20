package com.vityazev_egor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CommandsProcessor;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.IWaitTask;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
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
            "--disable-gpu",
            "--window-size=1280,1060"
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

    public Boolean loadUrlAndWait(String url, Integer timeOutSeconds){
        loadUrl(url);
        Shared.sleep(500); // we need give some time for browser to start loading of page
        WaitTask task = new WaitTask(new IWaitTask() {

            @Override
            public Boolean execute() {
                socketClient.sendCommandAsync(cmdProcessor.genExecuteJs("document.readyState"));
                String response = socketClient.waitResult(2);
                String result = cmdProcessor.getJsResult(response);
                if (result == null) return false;

                return result.equals("complete");
            }
            
        });
        return task.execute(timeOutSeconds, 500);
    }

    public void emualteMouseMove(Integer x, Integer y){
        socketClient.sendCommand(cmdProcessor.genMouseMove(x, y));
    }

    public Boolean loadUrlAndBypassCF(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        if (!loadResult) return false;
        String html = getHtml();
        if (html.contains("ray-id")){
            logger.warning("Detected CloudFlare");
            for (int j=0; j<20; j++){
                int[][] events = {
                    {96, 0, 424, 0}, {98, 7, 440, 0}, {100, 15, 456, 0}, {102, 23, 470, 0},
                    {103, 32, 490, 0}, {106, 43, 506, 0}, {107, 56, 522, 0}, {112, 73, 538, 0},
                    {115, 91, 555, 0}, {118, 108, 573, 0}, {121, 127, 587, 0}, {125, 145, 606, 0},
                    {127, 160, 622, 0}, {130, 169, 639, 0}, {132, 177, 655, 0}, {133, 184, 670, 0},
                    {135, 191, 689, 0}, {137, 196, 704, 0}, {139, 205, 722, 0}, {142, 213, 739, 0},
                    {145, 221, 755, 0}, {150, 233, 772, 0}, {155, 245, 789, 0}, {159, 258, 805, 0},
                    {162, 267, 828, 0}, {163, 269, 844, 0}, {163, 271, 861, 0}, {165, 273, 880, 0},
                    {168, 276, 897, 0}, {169, 277, 907, 0}, {170, 278, 924, 0}, {171, 278, 989, 0},
                    {172, 278, 995, 0}, {174, 279, 1010, 0}, {179, 281, 1030, 0}, {182, 283, 1046, 0},
                    {187, 285, 1060, 0}, {190, 288, 1074, 0}, {191, 290, 1091, 0}, {192, 290, 1103, 0},
                    {192, 290, 1310, 1}, {192, 290, 1385, 1}
                };
                int clickCount = 0;
                for (int i = 0; i < events.length; i++) {
                    int x = (int) events[i][0];
                    int y = (int) events[i][1];
                    int timestamp = (int) events[i][2];
                    int isClick = (int) events[i][3];
                    
                    // Рассчитываем время ожидания
                    long sleepTime = (i == 0) ? 0 : (timestamp - (int) events[i - 1][2]);
                    
                    // Ждем перед выполнением следующего события
                    Shared.sleep(sleepTime);
                    
                    // Эмулируем движение мыши
                    emualteMouseMove(x, y);
                    
                    // Проверяем, нужно ли эмулировать клик
                    if (isClick == 1) {
                        if (clickCount == 0){
                            String cmd = cmdProcessor.genMouseClick(x, y)[0];
                            socketClient.sendCommand(cmd);
                            clickCount++;
                        }
                        else{
                            String cmd = cmdProcessor.genMouseClick(x, y)[1];
                            socketClient.sendCommand(cmd);
                        }
                        //mulateClick(x, y);
                    }
                }
                // var points = HumanMouseMoveGen.generateCurve(0, 0, 190, 287, 50);
                // for (Point p : points){
                //     emualteMouseMove(p.x, p.y);
                //     Shared.sleep(50);
                // }
                // //socketClient.sendCommand(cmdProcessor.genMouseMove(190, 287));
                // emulateClick(190, 287);
                // Shared.sleep(2000);
            }
            return false;
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }

    @Nullable
    public String getHtml(){
        String json = cmdProcessor.genExecuteJs("document.documentElement.outerHTML");
        socketClient.sendCommandAsync(json);
        return cmdProcessor.getJsResult(socketClient.waitResult(2));
    }

    @Nullable
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
