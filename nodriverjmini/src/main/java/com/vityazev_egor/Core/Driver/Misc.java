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
import com.vityazev_egor.Core.CDPCommandBuilder;
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
        String command = CDPCommandBuilder.create("Page.captureScreenshot")
            .addParam("format", "png")
            .build();
        
        var response = driver.getSocketClient().sendAndWaitResult(2, command);
        if (response.isEmpty()) return Optional.empty();
        var baseData = CDPCommandBuilder.getScreenshotData(response.get());
        if (baseData.isEmpty()) return Optional.empty();
        
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
        String command = CDPCommandBuilder.create("Network.clearBrowserCookies").build();
        driver.getSocketClient().sendCommand(command);
    }

    /**
     * Enables or disables Content Security Policy (CSP) bypass.
     * When enabled, this allows loading external scripts and modifying restricted content.
     *
     * @param enabled {@code true} to enable CSP bypass, {@code false} to disable it.
     */
    public void setBypassCSP(Boolean enabled){
        String command = CDPCommandBuilder.create("Page.setBypassCSP")
            .addParam("enabled", enabled)
            .build();
        driver.getSocketClient().sendCommand(command);
    }
}
