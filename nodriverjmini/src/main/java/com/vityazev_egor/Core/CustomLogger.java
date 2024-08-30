package com.vityazev_egor.Core;

import java.util.logging.Logger;

public class CustomLogger {
    public final Logger logger;
    private final String ANSI_RESET = "\u001B[0m";
    private final String ANSI_RED = "\u001B[31m";
    private final String ANSI_GREEN = "\u001B[32m";
    private final String ANSI_YELLOW = "\u001B[33m";

    public CustomLogger(String name){
        logger = Logger.getLogger(name);
    }

    public void info(String message){
        logger.info(ANSI_GREEN + message + ANSI_RESET);
    }

    public void warning(String message){
        logger.warning(ANSI_YELLOW + message + ANSI_RESET);
    }

    public void error(String message, Exception ex){
        logger.warning(ANSI_RED + message + ANSI_RESET);
        ex.printStackTrace();
    }
}
