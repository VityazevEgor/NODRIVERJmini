package com.vityazev_egor.Core.Driver;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import org.apache.commons.imaging.Imaging;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CustomLogger;

public class Misc {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Misc(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(this.getClass().getName());
    }


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

    public Optional<BufferedImage> captureScreenshot(){
        return captureScreenshot(null);
    }

    public void clearCookies(){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genClearCookies());
    }
}
