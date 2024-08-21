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

import java.awt.*;

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
            "--window-size=1280,1060",
            "--no-first-run",
            "--user-data-dir=nodriverData"
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

    // works even if u use proxy
    public Boolean loadUrlAndBypassCFXDO(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        if (!loadResult) return false;
        String html = getHtml();
        if (html.contains("ray-id")){
            logger.warning("Detected CloudFlare");
            for (int j=0; j<5; j++){
                xdoClick(180, 270);
                Shared.sleep(2000);
            }
            return false;
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }

    // works only if ur IP is not related to hsoting IPs
    public Boolean loadUrlAndBypassCFCDP(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        //socketClient.sendCommand(cmdProcessor.genMouseMove(190, 287));
        if (!loadResult) return false;
        String html = getHtml();
        if (html.contains("ray-id")){
            logger.warning("Detected CloudFlare");
            //Shared.sleep(10000);
            for (int tries = 0; tries<10; tries++){
                // в начале получаем текущее время с момента загрузки страницы
                Double currentPageTime = getCurrentPageTime();
                if (currentPageTime != null){
                    socketClient.sendCommand(cmdProcessor.genMouseClick(190, 287, currentPageTime));
                }
                else{
                    logger.warning("Can't get currentPage time");
                }
                Shared.sleep(2000);
            }
            return false;
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }

    @Nullable
    public Double getCurrentPageTime(){
        String json = cmdProcessor.genExecuteJs("performance.now()");
        socketClient.sendCommandAsync(json);
        Double result = null;
        String response = cmdProcessor.getJsResult(socketClient.waitResult(2));
        if (response !=null){
            result = Double.parseDouble(response);
        }

        return result;
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

    @Nullable
    public Dimension getViewPortSize(){
        String[] jsons = new String[]{
            cmdProcessor.genExecuteJs("window.innerWidth"),
            cmdProcessor.genExecuteJs("window.innerHeight")
        };
        String[] result = new String[2];
        for (int i=0; i<jsons.length; i++){
            socketClient.sendCommandAsync(jsons[i]);
            result[i] = cmdProcessor.getJsResult(socketClient.waitResult(2));
        }
        if (result[0] != null && result[1] != null){
            return new Dimension(Integer.parseInt(result[0]), Integer.parseInt(result[1]));
        }
        else{
            return null;
        }
    }

    public void emulateClick(Integer x, Integer y){
        String[] json = cmdProcessor.genMouseClick(x, y);
        socketClient.sendCommand(json[0]);
        socketClient.sendCommand(json[1]);
    }

    public void xdoClick(Integer x, Integer y) {
        // Получаем позицию окна на экране
        Dimension windowPosition = getWindowPosition(); // Позиция окна на экране (начало отчёта координат)
        
        // Получаем размер видимой части браузера (viewport)
        Dimension viewPortSize = getViewPortSize(); // Размер viewport (например, 1236x877)
        
        // Определите размеры окна браузера, которые вы можете использовать для расчета
        Dimension browserWindowSize = new Dimension(1280, 1060); // Размер окна браузера
    
        // Получаем размеры интерфейса браузера
        // Интерфейс браузера — это то, что находится за пределами видимой части браузера
        double interfaceHeight = browserWindowSize.getHeight() - viewPortSize.getHeight(); // должно быть +-142
        double interfaceWidth = browserWindowSize.getWidth() - viewPortSize.getWidth();
        
        // Размер экрана, который используется ydotool (фактически установленный размер)
        Dimension ydotoolScreenSize = new Dimension(1030, 580); // Размер экрана, используемый ydotool

        // Масштабный коэффициент для преобразования координат из размера экрана ydotool в реальный размер
        double scaleX = (double) ydotoolScreenSize.width / 1920;
        double scaleY = (double) ydotoolScreenSize.height / 1080;
        
        // Проверьте полученные значения для отладки
        logger.warning("Interface height = " + interfaceHeight); 
        logger.warning("Interface width = " + interfaceWidth);
    
        // Координаты клика на экране:
        // Позиция окна на экране + отступы из-за интерфейса
        Integer screenX = (int) windowPosition.getWidth() + (int) interfaceWidth + x;
        Integer screenY = (int) windowPosition.getHeight() + (int) interfaceHeight + y;
    
        // Расчет координат клика на экране
        logger.warning(String.format("Screen click pos %d %d", screenX, screenY));

        // Преобразуем координаты в координаты для ydotool
        Integer ydotoolX = (int) (screenX * scaleX);
        Integer ydotoolY = (int) (screenY * scaleY);

    
        // Формируем команду для клика с помощью xdotool
        String moveCmd = String.format("ydotool mousemove %d %d", ydotoolX, ydotoolY);
        String clickCmd = "xdotool click 1";
        
        // Выполняем команды
        new ConsoleListener(moveCmd).run();
        new ConsoleListener(clickCmd).run();
        //socketClient.sendCommand(cmdProcessor.genLayoutMetrics());
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

    @Nullable
    public Dimension getWindowPosition() {
        String currentTitle = getTitle();
        // Команда для поиска окон по процессу Chrome
        String searchCmd = "xdotool search --pid " + chrome.pid();
        
        // Выполняем команду поиска окон
        var searchListener = new ConsoleListener(searchCmd);
        searchListener.run();
        List<String> windowIds = searchListener.getConsoleMessages();
        
        String matchingId = null;
        for (String windowId : windowIds) {
            // Команда для получения заголовка окна по ID
            String getNameCmd = "xdotool getwindowname " + windowId;
            var nameListener = new ConsoleListener(getNameCmd);
            nameListener.run();
            String windowTitle = nameListener.getConsoleMessages().get(0);
            
            if (windowTitle.contains(currentTitle)) {
                matchingId = windowId;
                break; // Найдено подходящее окно, можем завершить поиск
            }
        }
        
        if (matchingId == null) {
            System.out.println("Window with title \"" + currentTitle + "\" not found.");
            return null;
        }
        
        // Команда для получения геометрии окна по ID
        String getGeometryCmd = "xdotool getwindowgeometry " + matchingId;
        var geometryListener = new ConsoleListener(getGeometryCmd);
        geometryListener.run();
        String geometryOutput = geometryListener.getConsoleMessages().get(1);
        
        // Извлекаем позицию окна из вывода команды
        String position = geometryOutput.replace("  Position: ", "").replace(" (screen: 0)", "");
        System.out.println(position);
        
        // Преобразуем строку позиции в объект Dimension
        String[] parts = position.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        return new Dimension(x, y);
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
