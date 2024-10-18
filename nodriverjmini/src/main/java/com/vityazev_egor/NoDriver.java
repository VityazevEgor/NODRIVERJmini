package com.vityazev_egor;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.apache.commons.imaging.Imaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.Core.CommandsProcessor;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WaitTask.IWaitTask;
import com.vityazev_egor.Core.WebSocketClient;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.WebElements.WebElement;
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

    // переменные, который используются для корректировки нажатий через xdo
    private Integer calibrateX = 0, calibrateY = 0;

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

    // метод, который исправляет точность определения координат клика с использованием xdotool
    public Boolean calibrateXDO(){
        InputStream resInputStream = getClass().getClassLoader().getResourceAsStream("calibrateTest.html");
        Path pathToTestHtml = Paths.get(System.getProperty("user.home"), "calibrateTest.html");
        if (!Files.exists(pathToTestHtml)){
            try {
                Files.copy(resInputStream, pathToTestHtml);
            } catch (IOException e) {
                logger.error("Error while copying calibrateTest.html. Can't do calibration", e);
                return false;
            }
        }

        loadUrlAndWait("file:///"+pathToTestHtml.toString(), 5);
        xdoClick(100, 100);
        var xDivContent = findElement(By.id("xdata")).getContent(this);
        var yDivContent = findElement(By.id("ydata")).getContent(this);
        if (!xDivContent.isPresent() || !yDivContent.isPresent()) return false;
        logger.warning(String.format("Real x = %s; Real y = %s", xDivContent.get(), yDivContent.get()));

        calibrateX = Integer.parseInt(xDivContent.get()) - 100;
        calibrateY = Integer.parseInt(yDivContent.get()) - 100;
        logger.warning(String.format("Diff x = %s; Diff y = %s", calibrateX.toString(), calibrateY.toString()));

        try {
            Files.delete(pathToTestHtml);
        } catch (IOException e) {
            logger.error("Can't delete test html file", e);
        }
        return true;
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
                Optional<String> response = socketClient.sendAndWaitResult(2, cmdProcessor.genExecuteJs("document.readyState"));
                if (!response.isPresent()) return false;
                String jsResult = cmdProcessor.getJsResult(response.get());

                return jsResult.equals("complete");
            }
            
        });
        return task.execute(timeOutSeconds, 500);
    }

    public void emualteMouseMove(Integer x, Integer y){
        socketClient.sendCommand(cmdProcessor.genMouseMove(x, y));
    }

    private Optional<Point> getElementPosition(WebElement element){
        return element.getPosition(this);
    }
    private Optional<Dimension> getElementSize(WebElement element){
        return element.getSize(this);
    }

    // works even if u use proxy
    public Boolean loadUrlAndBypassCFXDO(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        if (!loadResult) return false;

        var html = getHtml();
        if (!html.isPresent()) return false;

        if (html.get().contains("ray-id")){
            logger.warning("Detected CloudFlare");
            var task = new WaitTask(
                new IWaitTask() {

                    @Override
                    public Boolean execute() {
                        var currentHtml = getHtml();
                        if (!currentHtml.isPresent()) return false;

                        if (currentHtml.get().contains("ray-id")){
                            var spacer = findElement(By.cssSelector("div[style*=\"display: grid;\"]"));
                            var spacerPoint = getElementPosition(spacer);
                            var spacerSize = getElementSize(spacer);
                            if (!spacerPoint.isPresent() || !spacerSize.isPresent()){
                                logger.info("Spacer div is still loading...");
                                return false;
                            }
                            Integer realX = spacerPoint.get().x - spacerSize.get().width/2 + 30;
                            xdoClick(realX, spacerPoint.get().y);
                            //xdoClick(207, 286);
                            return false;
                        }
                        else{
                            return true;
                        }
                    }
                    
                }
            );
            return task.execute(cfBypassTimeOutSeconds, 1000);
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }

    // socketClient.sendCommand(cmdProcessor.genMouseClick(190, 287, currentPageTime.get()));

    // works only if ur IP is not related to hsoting IPs
    public Boolean loadUrlAndBypassCFCDP(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        if (!loadResult) return false;

        var html = getHtml();
        if (!html.isPresent()) return false;

        if (html.get().contains("ray-id")){
            logger.warning("Detected CloudFlare");
            var task = new WaitTask(
                new IWaitTask() {

                    @Override
                    public Boolean execute() {
                        var currentHtml = getHtml();
                        if (!currentHtml.isPresent()) return false;
                        var currentPageTime = getCurrentPageTime();
                        if (!currentPageTime.isPresent()) return false;

                        if (currentHtml.get().contains("ray-id")){
                            socketClient.sendCommand(cmdProcessor.genMouseClick(190, 287, currentPageTime.get()));
                            return false;
                        }
                        else{
                            return true;
                        }
                    }
                    
                }
            );
            return task.execute(cfBypassTimeOutSeconds, 1000);
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }

    public Optional<Double> getCurrentPageTime(){
        var sDouble = getJSResult("performance.now()");
        if (sDouble.isPresent()){
            return Optional.of(Double.parseDouble(sDouble.get()));
        }
        else{
            return Optional.empty();
        }
    }


    public Optional<String> getHtml(){
        return getJSResult("document.documentElement.outerHTML");
    }

    public Optional<String> getTitle(){
        return getJSResult("document.title");
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

    public void emulateClick(Integer x, Integer y){
        String[] json = cmdProcessor.genMouseClick(x, y);
        socketClient.sendCommand(json[0]);
        socketClient.sendCommand(json[1]);
    }

    public void emulateClick(Double x, Double y){
        emulateClick(x.intValue(), y.intValue());
    }

    public void emulateClick(WebElement element){
        var position = element.getPosition(this);
        if (!position.isPresent()){
            logger.warning("Can't get element position to click");
            return;
        }

        emulateClick(position.get().getX(), position.get().getY());
    }

    public void xdoClick(Integer x, Integer y) {
        // Получаем позицию окна на экране
        Point windowPosition = getWindowPosition().get(); // Позиция окна на экране (начало отчёта координат)
        
        // Получаем размер видимой части браузера (viewport)
        Dimension viewPortSize = getViewPortSize().get(); // Размер viewport (например, 1236x877)
        
        // Определите размеры окна браузера, которые вы можете использовать для расчета
        Dimension browserWindowSize = new Dimension(1280, 1060); // Размер окна браузера
    
        // Получаем размеры интерфейса браузера
        // Интерфейс браузера — это то, что находится за пределами видимой части браузера
        double interfaceHeight = browserWindowSize.getHeight() - viewPortSize.getHeight(); // должно быть +-142
        double interfaceWidth = browserWindowSize.getWidth() - viewPortSize.getWidth();
        
        // Проверьте полученные значения для отладки
        logger.warning("Interface height = " + interfaceHeight); 
        logger.warning("Interface width = " + interfaceWidth);
    
        // Координаты клика на экране:
        // Позиция окна на экране + отступы из-за интерфейса
        Integer screenX = (int) windowPosition.getX() + (int) interfaceWidth + x - calibrateX;
        Integer screenY = (int) windowPosition.getY() + (int) interfaceHeight + y - calibrateY;
    
        // Расчет координат клика на экране
        logger.warning(String.format("Screen click pos %d %d", screenX, screenY));
    
        // Формируем команду для клика с помощью xdotool
        String moveCmd = String.format("xdotool mousemove %d %d", screenX, screenY);
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

    public void enterText(WebElement element, String text){
        element.getFocus(this);
        var list = cmdProcessor.genTextInput(text);
        socketClient.sendCommand(list);
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

    public Optional<String> getJSResult(String js){
        String json = cmdProcessor.genExecuteJs(js);
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

    public Optional<Point> getWindowPosition() {
        String currentTitle = getTitle().get();
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
                break; // Найдено подходящее окно, ливаем
            }
        }
        
        if (matchingId == null) {
            System.out.println("Window with title \"" + currentTitle + "\" not found.");
            return Optional.empty();
        }
        
        // Команда для получения геометрии окна по ID
        String getGeometryCmd = "xdotool getwindowgeometry " + matchingId;
        var geometryListener = new ConsoleListener(getGeometryCmd);
        geometryListener.run();
        String geometryOutput = geometryListener.getConsoleMessages().get(1);
        
        // Извлекаем позицию окна из вывода команды
        String position = geometryOutput.replace("  Position: ", "").replace(" (screen: 0)", "");
        System.out.println(position);
        
        // Преобразуем строку позиции в объект Point
        String[] parts = position.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        return Optional.of(new Point(x, y));
    }    

    public void testElementLocation(String elementId){
        String js = cmdProcessor.getElementLocation.replace("ELEMENT_ID", elementId);
        executeJS(js);
    }

    public WebElement findElement(By by){
        return new WebElement(by);
    }
    
    public void exit(){
        logger.warning("Destroying chrome");
        chrome.destroy();
        isInit = false;
    }
}
