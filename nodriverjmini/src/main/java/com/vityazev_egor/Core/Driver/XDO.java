package com.vityazev_egor.Core.Driver;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.ConsoleListener;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebElements.By;

public class XDO {
    private final NoDriver driver;
    private final CustomLogger logger;

    public XDO(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(XDO.class.getName());
    }

    public Boolean calibrate(){
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

        driver.getNavigation().loadUrlAndWait("file:///"+pathToTestHtml.toString(), 5);
        click(100, 100);
        var xDivContent = driver.findElement(By.id("xdata")).getHTMLContent();
        var yDivContent = driver.findElement(By.id("ydata")).getHTMLContent();
        if (!xDivContent.isPresent() || !yDivContent.isPresent()) return false;
        logger.warning(String.format("Real x = %s; Real y = %s", xDivContent.get(), yDivContent.get()));

        driver.setCalibrateX(Integer.parseInt(xDivContent.get()) - 100);
        driver.setCalibrateY(Integer.parseInt(yDivContent.get()) - 100);
        logger.warning(String.format("Diff x = %s; Diff y = %s", driver.getCalibrateX().toString(), driver.getCalibrateY().toString()));

        try {
            Files.delete(pathToTestHtml);
        } catch (IOException e) {
            logger.error("Can't delete test html file", e);
        }
        return true;
    }

    public void click(Integer x, Integer y) {
        // Получаем позицию окна на экране
        Point windowPosition = getWindowPosition().get(); // Позиция окна на экране (начало отчёта координат)
        
        // Получаем размер видимой части браузера (viewport)
        Dimension viewPortSize = driver.getViewPortSize().get(); // Размер viewport (например, 1236x877)
        
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
        Integer screenX = (int) windowPosition.getX() + (int) interfaceWidth + x - driver.getCalibrateX();
        Integer screenY = (int) windowPosition.getY() + (int) interfaceHeight + y - driver.getCalibrateY();
    
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

    public void click(Double x, Double y){
        click(x.intValue(), y.intValue());
    }

    public Optional<Point> getWindowPosition() {
        String currentTitle = driver.getTitle().get();
        // Команда для поиска окон по процессу Chrome
        String searchCmd = "xdotool search --pid " + driver.getChrome().pid();
        
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
}
