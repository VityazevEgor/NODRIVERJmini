package com.vityazev_egor.Core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import lombok.Getter;

public class ConsoleListener implements Runnable{
    private final Process process;
    private final boolean debugMode;
    private final CountDownLatch initLatch;
    private final CustomLogger logger;
    @Getter
    private List<String> consoleMessages = new ArrayList<>();

    public ConsoleListener(Process process, boolean debugMode, CountDownLatch initLatch){
        this.process = process;
        this.debugMode = debugMode;
        this.initLatch = initLatch;
        this.logger = new CustomLogger(ConsoleListener.class.getName());
    }

    @Override
    public void run() {
        if (process == null) return;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean initialized = false;
            
            while ((line = reader.readLine()) != null) {
                if (debugMode) {
                    logger.info("Chrome: " + line);
                }
                
                // Сигнализируем об инициализации при первом сообщении от Chrome
                if (!initialized) {
                    initialized = true;
                    initLatch.countDown();
                }
            }
            
            int exitCode = process.waitFor();
            if (debugMode) {
                logger.info("Chrome process exited with code: " + exitCode);
            }
        } catch (Exception e) {
            logger.error("Error in Chrome process monitoring", e);
            // Сигнализируем об ошибке инициализации
            initLatch.countDown();
        }
    }

    public long getPid(){
        return process.pid();
    }    
}
