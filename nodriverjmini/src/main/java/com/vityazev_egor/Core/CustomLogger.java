package com.vityazev_egor.Core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CustomLogger {
    private final String className;
    private final String ANSI_RESET = "\u001B[0m";
    private final String ANSI_RED = "\u001B[31m";
    private final String ANSI_GREEN = "\u001B[32m";
    private final String ANSI_YELLOW = "\u001B[33m";
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public CustomLogger(String name) {
        this.className = name;
    }

    private String formatMessage(String level, String color, String message) {
        String time = LocalTime.now().format(timeFormatter);
        return String.format("%s[%s] [%s] %s: %s%s", color, time, className, level, message, ANSI_RESET);
    }

    public void info(String message) {
        System.out.println(formatMessage("INFO", ANSI_GREEN, message));
    }

    public void warning(String message) {
        System.out.println(formatMessage("WARNING", ANSI_YELLOW, message));
    }

    public void error(String message, Exception ex) {
        System.out.println(formatMessage("ERROR", ANSI_RED, message));
        if (ex != null) {
            ex.printStackTrace();
        }
    }
}
