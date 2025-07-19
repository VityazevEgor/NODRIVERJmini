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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CommandRunner;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebElements.By;

public class XDO {
    private final NoDriver driver;
    private final CustomLogger logger;
    private final boolean isWindows;

    public XDO(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(XDO.class.getName());
        this.isWindows = isWindowsOS();
    }

    /**
     * Windows API interface for JNA access to user32.dll functions.
     */
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetCursorPos(int x, int y);
        void mouse_event(int dwFlags, int dx, int dy, int dwData, Pointer dwExtraInfo);

        int MOUSEEVENTF_LEFTDOWN = 0x0002;
        int MOUSEEVENTF_LEFTUP = 0x0004;
    }

    /**
     * Windows API interface for JNA access to kernel32.dll functions.
     */
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        
        int GetCurrentProcessId();
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
     * Calibrates the browser window position relative to the system cursor by using a test HTML file.
     *
     * @return {@code true} if calibration is successful, otherwise {@code false}.
     */
    public Boolean calibrate(){
        if (isWindows) {
            return calibrateWindows();
        } else {
            return calibrateLinux();
        }
    }

    /**
     * Calibrates the browser window position on Linux using xdotool.
     *
     * @return {@code true} if calibration is successful, otherwise {@code false}.
     */
    private Boolean calibrateLinux(){
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
        if (!click(100, 100)) {
            logger.error("Failed to perform calibration click");
            return false;
        }
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
     * Calibrates the browser window position on Windows using PowerShell and Windows API calls.
     *
     * @return {@code true} if calibration is successful, otherwise {@code false}.
     */
    private Boolean calibrateWindows(){
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
        if (!click(100, 100)) {
            logger.error("Failed to perform calibration click");
            return false;
        }
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
     * @return {@code true} if click is successful, otherwise {@code false}.
     */
    public Boolean click(Integer x, Integer y) {
        if (isWindows) {
            return clickWindows(x, y);
        } else {
            return clickLinux(x, y);
        }
    }

    /**
     * Simulates a mouse click on Linux using xdotool.
     *
     * @param x The X coordinate relative to the browser viewport.
     * @param y The Y coordinate relative to the browser viewport.
     * @return {@code true} if click is successful, otherwise {@code false}.
     */
    private Boolean clickLinux(Integer x, Integer y) {
        try {
            Point windowPosition = getWindowPositionLinux().orElseThrow(() -> new RuntimeException("Could not get browser window position"));
            Dimension viewPortSize = driver.getViewPortSize().orElseThrow(() -> new RuntimeException("Could not get viewport size"));
            
            Dimension browserWindowSize = getBrowserWindowSizeLinux().orElseThrow(() -> new RuntimeException("Could not get browser window size"));
        
            double interfaceHeight = browserWindowSize.getHeight() - viewPortSize.getHeight();
            double interfaceWidth = browserWindowSize.getWidth() - viewPortSize.getWidth();
            
            logger.warning("Interface height = " + interfaceHeight); 
            logger.warning("Interface width = " + interfaceWidth);
        
            Integer screenX = (int) windowPosition.getX() + (int) interfaceWidth + x - driver.getCalibrateX();
            Integer screenY = (int) windowPosition.getY() + (int) interfaceHeight + y - driver.getCalibrateY();
        
            logger.warning(String.format("Screen click pos %d %d", screenX, screenY));
        
            String moveCmd = String.format("xdotool mousemove %d %d", screenX, screenY);
            String clickCmd = "xdotool click 1";
            
            CommandRunner.executeCommand(moveCmd);
            CommandRunner.executeCommand(clickCmd);
            
            return true;
        } catch (RuntimeException e) {
            logger.error("Error performing click: " + e.getMessage());
            return false;
        }
    }

    /**
     * Simulates a mouse click on Windows using JNA and Windows API.
     *
     * @param x The X coordinate relative to the browser viewport.
     * @param y The Y coordinate relative to the browser viewport.
     * @return {@code true} if click is successful, otherwise {@code false}.
     */
    private Boolean clickWindows(Integer x, Integer y) {
        try {
            Point windowPosition = getWindowPositionWindows().orElseThrow(() -> new RuntimeException("Could not get browser window position"));
            Dimension viewPortSize = driver.getViewPortSize().orElseThrow(() -> new RuntimeException("Could not get viewport size"));
            
            Dimension browserWindowSize = getBrowserWindowSizeWindows().orElseThrow(() -> new RuntimeException("Could not get browser window size"));
        
            double interfaceHeight = browserWindowSize.getHeight() - viewPortSize.getHeight();
            double interfaceWidth = browserWindowSize.getWidth() - viewPortSize.getWidth();
            
            logger.warning("Interface height = " + interfaceHeight); 
            logger.warning("Interface width = " + interfaceWidth);
        
            int screenX = (int) windowPosition.getX() + (int) interfaceWidth + x - driver.getCalibrateX();
            int screenY = (int) windowPosition.getY() + (int) interfaceHeight + y - driver.getCalibrateY();
        
            logger.warning(String.format("Screen click pos %d %d", screenX, screenY));
        
            // Use JNA to call Windows API directly
            boolean setCursorSuccess = User32.INSTANCE.SetCursorPos(screenX, screenY);
            if (!setCursorSuccess) {
                logger.error("Failed to set cursor position");
                return false;
            }
            
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, null);
            User32.INSTANCE.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, null);
            
            return true;
        } catch (RuntimeException e) {
            logger.error("Error performing click: " + e.getMessage());
            return false;
        }
    }

    /**
     * Overloaded method to allow clicking with {@code double} coordinates.
     *
     * @param x The X coordinate relative to the browser viewport.
     * @param y The Y coordinate relative to the browser viewport.
     * @return {@code true} if click is successful, otherwise {@code false}.
     */
    public Boolean click(Double x, Double y){
        return click(x.intValue(), y.intValue());
    }

    /**
     * Retrieves the position of the browser window on the screen.
     *
     * @return An {@code Optional<Point>} containing the window's top-left position, or empty if not found.
     */
    private Optional<Point> getWindowPosition() {
        if (isWindows) {
            return getWindowPositionWindows();
        } else {
            return getWindowPositionLinux();
        }
    }

    /**
     * Retrieves the position of the browser window on Linux using xdotool.
     *
     * @return An {@code Optional<Point>} containing the window's top-left position, or empty if not found.
     */
    private Optional<Point> getWindowPositionLinux() {
        try {
            String currentTitle = driver.getTitle().orElseThrow(() -> new RuntimeException("Could not get browser title"));
            String searchCmd = "xdotool search --pid " + driver.getChrome().pid();
            
            List<String> windowIds = CommandRunner.executeCommand(searchCmd);
            
            String matchingId = null;
            for (String windowId : windowIds) {
                String getNameCmd = "xdotool getwindowname " + windowId;
                List<String> nameOutput = CommandRunner.executeCommand(getNameCmd);
                if (!nameOutput.isEmpty()) {
                    String windowTitle = nameOutput.get(0);
                    
                    if (windowTitle.contains(currentTitle)) {
                        matchingId = windowId;
                        break;
                    }
                }
            }
            
            if (matchingId == null) {
                logger.warning("Window with title \"" + currentTitle + "\" not found.");
                return Optional.empty();
            }
            
            String getGeometryCmd = "xdotool getwindowgeometry " + matchingId;
            List<String> geometryOutput = CommandRunner.executeCommand(getGeometryCmd);
            if (geometryOutput.size() < 2) {
                logger.warning("Invalid geometry output from xdotool");
                return Optional.empty();
            }
            String geometryLine = geometryOutput.get(1);
            
            String position = geometryLine.replace("  Position: ", "").replace(" (screen: 0)", "");
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

    /**
     * Retrieves the position of the browser window on Windows using JNA and Windows API.
     *
     * @return An {@code Optional<Point>} containing the window's top-left position, or empty if not found.
     */
    private Optional<Point> getWindowPositionWindows() {
        try {
            String currentTitle = driver.getTitle().orElseThrow(() -> new RuntimeException("Could not get browser title"));
            
            // Find window by title (partial match)
            WinDef.HWND hwnd = findWindowByTitle(currentTitle);
            
            if (hwnd == null) {
                logger.warning("Window with title \"" + currentTitle + "\" not found.");
                return Optional.empty();
            }
            
            // Get window rectangle using standard JNA User32
            WinDef.RECT rect = new WinDef.RECT();
            boolean success = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowRect(hwnd, rect);
            
            if (!success) {
                logger.warning("Failed to get window rectangle");
                return Optional.empty();
            }
            
            int x = rect.left;
            int y = rect.top;
            
            logger.info("Window position: " + x + "," + y);
            return Optional.of(new Point(x, y));
            
        } catch (Exception e) {
            logger.error("Error getting window position: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves the size of the browser window on Linux using xdotool.
     *
     * @return An {@code Optional<Dimension>} containing the window size, or empty if not found.
     */
    private Optional<Dimension> getBrowserWindowSizeLinux() {
        try {
            String currentTitle = driver.getTitle().orElseThrow(() -> new RuntimeException("Could not get browser title"));
            String searchCmd = "xdotool search --name \"" + currentTitle + "\"";
            
            List<String> windowIds = CommandRunner.executeCommand(searchCmd);
            
            if (windowIds.isEmpty()) {
                logger.warning("No window found with title \"" + currentTitle + "\"");
                return Optional.empty();
            }
            
            // Take the first window ID
            String firstWindowId = windowIds.get(0);
            
            String getGeometryCmd = "xdotool getwindowgeometry " + firstWindowId;
            List<String> geometryOutput = CommandRunner.executeCommand(getGeometryCmd);
            
            if (geometryOutput.size() < 3) {
                logger.warning("Invalid geometry output from xdotool");
                return Optional.empty();
            }
            
            // Parse "Geometry: 1495x818" from the third line
            String geometryLine = geometryOutput.get(2);
            String geometry = geometryLine.replace("  Geometry: ", "");
            
            String[] parts = geometry.split("x");
            if (parts.length != 2) {
                logger.warning("Failed to parse geometry: " + geometry);
                return Optional.empty();
            }
            
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            
            logger.info("Browser window size: " + width + "x" + height);
            return Optional.of(new Dimension(width, height));
        } catch (Exception e) {
            logger.error("Error getting browser window size: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves the size of the browser window on Windows using Windows API.
     *
     * @return An {@code Optional<Dimension>} containing the window size, or empty if not found.
     */
    private Optional<Dimension> getBrowserWindowSizeWindows() {
        try {
            String currentTitle = driver.getTitle().orElseThrow(() -> new RuntimeException("Could not get browser title"));
            
            // Find window by title
            WinDef.HWND hwnd = findWindowByTitle(currentTitle);
            
            if (hwnd == null) {
                logger.warning("Window with title \"" + currentTitle + "\" not found.");
                return Optional.empty();
            }
            
            // Get window rectangle using standard JNA User32
            WinDef.RECT rect = new WinDef.RECT();
            boolean success = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowRect(hwnd, rect);
            
            if (!success) {
                logger.warning("Failed to get window rectangle");
                return Optional.empty();
            }
            
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;
            
            logger.info("Browser window size: " + width + "x" + height);
            return Optional.of(new Dimension(width, height));
            
        } catch (Exception e) {
            logger.error("Error getting browser window size: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves the size of the browser window.
     *
     * @return An {@code Optional<Dimension>} containing the window size, or empty if not found.
     */
    public Optional<Dimension> getBrowserWindowSize() {
        if (isWindows) {
            return getBrowserWindowSizeWindows();
        } else {
            return getBrowserWindowSizeLinux();
        }
    }

    /**
     * Finds a window by partial title match.
     *
     * @param partialTitle The partial title to search for
     * @return HWND of the found window, or null if not found
     */
    private WinDef.HWND findWindowByTitle(String partialTitle) {
        final WinDef.HWND[] foundWindow = {null};
        
        // Enumerate all windows using standard JNA User32
        WinUser.WNDENUMPROC enumProc = new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hWnd, Pointer data) {
                char[] windowText = new char[512];
                com.sun.jna.platform.win32.User32.INSTANCE.GetWindowText(hWnd, windowText, 512);
                String title = Native.toString(windowText);
                
                if (title.contains(partialTitle)) {
                    foundWindow[0] = hWnd;
                    return false; // Stop enumeration
                }
                return true; // Continue enumeration
            }
        };
        
        com.sun.jna.platform.win32.User32.INSTANCE.EnumWindows(enumProc, null);
        return foundWindow[0];
    }
}
