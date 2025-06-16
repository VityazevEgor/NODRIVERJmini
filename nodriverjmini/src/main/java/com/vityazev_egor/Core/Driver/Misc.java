package com.vityazev_egor.Core.Driver;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.imaging.Imaging;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

public class Misc {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Misc(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(this.getClass().getName());
    }


    /**
     * Captures a screenshot of the current tab.
     * The screenshot is retrieved as a Base64-encoded PNG image, decoded, and converted into a {@link BufferedImage}.
     * If a valid file path is provided, the image is also saved to the specified location.
     *
     * @param screenSavePath The path where the screenshot should be saved. If {@code null}, the image is not saved.
     * @return An {@link Optional} containing the captured screenshot as a {@link BufferedImage}, or an empty {@link Optional} if capturing fails.
     */
    public Optional<BufferedImage> captureScreenshot(Path screenSavePath){
        var response = driver.getSocketClient().sendAndWaitResult(2, driver.getCmdProcessor().genCaptureScreenshot());
        if (!response.isPresent()) return Optional.empty();
        var baseData = driver.getCmdProcessor().getScreenshotData(response.get());
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

    /**
     * Captures a screenshot of the current browser window without saving it to a file.
     *
     * @return An {@link Optional} containing the captured screenshot as a {@link BufferedImage}, or an empty {@link Optional} if capturing fails.
     */
    public Optional<BufferedImage> captureScreenshot(){
        return captureScreenshot(null);
    }

    /**
     * Clears all cookies stored in the browser session.
     */
    public void clearCookies(){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genClearCookies());
    }

    /**
     * Enables or disables Content Security Policy (CSP) bypass.
     * When enabled, this allows loading external scripts and modifying restricted content.
     *
     * @param enabled {@code true} to enable CSP bypass, {@code false} to disable it.
     */
    public void setBypassCDP(Boolean enabled){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genBypassCSP(enabled));
    }

    public Optional<BufferedImage> htmlToImage(Optional<String> optionalHTML) {
        if (!optionalHTML.isPresent()) return Optional.empty();
        String html = optionalHTML.get();
        try {
            // Генерация временного HTML файла
            String htmlFileName = UUID.randomUUID().toString() + ".html";
            Path htmlFilePath = Paths.get(System.getProperty("user.dir"), htmlFileName).toAbsolutePath();
            String htmlPageContent = Shared.readResource("fullPageScreen.html")
                    .orElseThrow(() -> new IllegalStateException("Template not found"))
                    .replace("PASTEHTMLHERE", html);
            Files.writeString(htmlFilePath, htmlPageContent);
    
            logger.warning("File created: " + htmlFilePath);
            
            // Загрузка файла в браузер
            driver.getNavigation().loadUrlAndWait("file://" + htmlFilePath, 5);
    
            // Выполнение скрипта для захвата скриншота
            Shared.sleep(1000);
            String captureJS = Shared.readResource("miscJS/capturePage.js")
                    .orElseThrow(() -> new IllegalStateException("JS script not found"));
            driver.executeJS(captureJS);
    
            // Ожидание, пока base64 строка не будет готова
            var base64Div = driver.findElement(By.id("base64image"));
            WaitTask waitTask = new WaitTask() {
                @Override
                public Boolean condition() {
                    return base64Div.isExists() && base64Div.getText().filter(text -> text.length() > 4).isPresent();
                }
            };
    
            if (!waitTask.execute(2, 100)) {
                return Optional.empty();
            }
    
            // Декодирование base64 в изображение
            byte[] imageBytes = Base64.getDecoder().decode(base64Div.getText().orElseThrow());
            Files.deleteIfExists(htmlFilePath);
    
            return Optional.of(Imaging.getBufferedImage(imageBytes));
        } catch (Exception ex) {
            logger.error("Failed to capture HTML as image", ex);
            return Optional.empty();
        }
    }
    
}
