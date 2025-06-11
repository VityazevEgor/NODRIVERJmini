package com.vityazev_egor.Core.Driver;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
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

    /**
     * Calibrates the browser window position relative to the system cursor by using a test HTML file.
     *
     * @return {@code true} if calibration is successful, otherwise {@code false}.
     */
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

        driver.getNavigation().loadUrlAndWait("file:///"+ pathToTestHtml, 5);
        click(100, 100);
        try {
            var xDivContent = driver.findElement(By.id("xdata")).getHTMLContent().orElseThrow(() -> new RuntimeException("Could not get X coordinate data"));
            var yDivContent = driver.findElement(By.id("ydata")).getHTMLContent().orElseThrow(() -> new RuntimeException("Could not get Y coordinate data"));
            
            logger.warning(String.format("Real x = %s; Real y = %s", xDivContent, yDivContent));

            driver.setCalibrateX(Integer.parseInt(xDivContent) - 100);
            driver.setCalibrateY(Integer.parseInt(yDivContent) - 100);
        } catch (RuntimeException e) {
            logger.error("Error during calibration: " + e.getMessage());
            return false;
        }
        logger.warning(String.format("Diff x = %s; Diff y = %s", driver.getCalibrateX().toString(), driver.getCalibrateY().toString()));

        try {
            Files.delete(pathToTestHtml);
        } catch (IOException e) {
            logger.error("Can't delete test html file", e);
        }
        return true;
    }

    /**
     * Simulates a mouse click at the specified coordinates on the screen, adjusting for browser interface offsets.
     *
     * @param x The X coordinate relative to the browser viewport.
     * @param y The Y coordinate relative to the browser viewport.
     */
    public void click(Integer x, Integer y) {
        try {
            Point windowPosition = getWindowPosition().orElseThrow(() -> new RuntimeException("Could not get browser window position"));
            Dimension viewPortSize = driver.getViewPortSize().orElseThrow(() -> new RuntimeException("Could not get viewport size"));
            
            Dimension browserWindowSize = new Dimension(1280, 1060);
        
            double interfaceHeight = browserWindowSize.getHeight() - viewPortSize.getHeight();
            double interfaceWidth = browserWindowSize.getWidth() - viewPortSize.getWidth();
            
            logger.warning("Interface height = " + interfaceHeight); 
            logger.warning("Interface width = " + interfaceWidth);
        
            Integer screenX = (int) windowPosition.getX() + (int) interfaceWidth + x - driver.getCalibrateX();
            Integer screenY = (int) windowPosition.getY() + (int) interfaceHeight + y - driver.getCalibrateY();
        
            logger.warning(String.format("Screen click pos %d %d", screenX, screenY));
        
            String moveCmd = String.format("xdotool mousemove %d %d", screenX, screenY);
            String clickCmd = "xdotool click 1";
            
            new ConsoleListener(moveCmd).run();
            new ConsoleListener(clickCmd).run();
        } catch (RuntimeException e) {
            logger.error("Error performing click: " + e.getMessage());
        }
    }

    /**
     * Overloaded method to allow clicking with {@code double} coordinates.
     *
     * @param x The X coordinate relative to the browser viewport.
     * @param y The Y coordinate relative to the browser viewport.
     */
    public void click(Double x, Double y){
        click(x.intValue(), y.intValue());
    }

    /**
     * Retrieves the position of the browser window on the screen.
     * <p>
     * This method searches for browser windows by process ID, matches the window
     * by title, and extracts the window's position coordinates from xdotool output.
     *
     * @return An {@code Optional<Point>} containing the window's top-left position, or empty if not found.
     */
    private Optional<Point> getWindowPosition() {
        try {
            String currentTitle = driver.getTitle().orElseThrow(() -> new RuntimeException("Could not get browser title"));
            String searchCmd = "xdotool search --pid " + driver.getChrome().pid();
            
            var searchListener = new ConsoleListener(searchCmd);
            searchListener.run();
            List<String> windowIds = searchListener.getConsoleMessages();
            
            String matchingId = null;
            for (String windowId : windowIds) {
                String getNameCmd = "xdotool getwindowname " + windowId;
                var nameListener = new ConsoleListener(getNameCmd);
                nameListener.run();
                String windowTitle = nameListener.getConsoleMessages().get(0);
                
                if (windowTitle.contains(currentTitle)) {
                    matchingId = windowId;
                    break;
                }
            }
            
            if (matchingId == null) {
                logger.warning("Window with title \"" + currentTitle + "\" not found.");
                return Optional.empty();
            }
            
            String getGeometryCmd = "xdotool getwindowgeometry " + matchingId;
            var geometryListener = new ConsoleListener(getGeometryCmd);
            geometryListener.run();
            String geometryOutput = geometryListener.getConsoleMessages().get(1);
            
            String position = geometryOutput.replace("  Position: ", "").replace(" (screen: 0)", "");
            logger.info("Window position: " + position);
            
            String[] parts = position.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            return Optional.of(new Point(x, y));
        } catch (Exception e) {
            logger.error("Error getting window position: " + e.getMessage());
            return Optional.empty();
        }
    }
}
