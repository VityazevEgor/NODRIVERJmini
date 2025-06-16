package com.vityazev_egor.Core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLogger {
    private final String className;
    
    // ANSI Color codes for better visual distinction
    private final String ANSI_RESET = "\u001B[0m";
    private final String ANSI_BOLD = "\u001B[1m";
    private final String ANSI_DIM = "\u001B[2m";
    
    // Text colors
    private final String ANSI_RED = "\u001B[31m";
    private final String ANSI_GREEN = "\u001B[32m";
    private final String ANSI_YELLOW = "\u001B[33m";
    private final String ANSI_BLUE = "\u001B[34m";
    private final String ANSI_MAGENTA = "\u001B[35m";
    private final String ANSI_CYAN = "\u001B[36m";
    private final String ANSI_WHITE = "\u001B[37m";
    
    // Background colors for level highlighting
    private final String ANSI_BG_RED = "\u001B[41m";
    private final String ANSI_BG_GREEN = "\u001B[42m";
    private final String ANSI_BG_YELLOW = "\u001B[43m";
    private final String ANSI_BG_BLUE = "\u001B[44m";
    
    // Log level control
    private final Boolean DEBUG = Boolean.parseBoolean(System.getProperty("debug", "false"));
    private final Boolean VERBOSE = Boolean.parseBoolean(System.getProperty("verbose", "false"));
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final String shortClassName;

    public CustomLogger(String name) {
        this.className = name;
        // Extract just the class name without package for cleaner display
        this.shortClassName = name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Formats log message with enhanced visual styling
     */
    private String formatMessage(String level, String levelColor, String levelBg, String icon, String message) {
        String time = LocalDateTime.now().format(timeFormatter);
        
        // Create styled level badge
        String levelBadge = String.format("%s%s %s %s", 
            levelBg, ANSI_BOLD + ANSI_WHITE, level, ANSI_RESET);
        
        // Format: [TIME] 🎯 LEVEL ClassName → message
        return String.format("%s[%s]%s %s %s %s%s%s → %s%s%s", 
            ANSI_DIM, time, ANSI_RESET,
            icon,
            levelBadge,
            levelColor + ANSI_BOLD, shortClassName, ANSI_RESET,
            levelColor, message, ANSI_RESET);
    }

    /**
     * Debug level logging - only shown when DEBUG=true
     */
    public void debug(String message) {
        if (DEBUG) {
            System.out.println(formatMessage("DEBUG", ANSI_CYAN, ANSI_BG_BLUE, "🔍", message));
        }
    }

    /**
     * Info level logging - shows general information
     */
    public void info(String message) {
        if (DEBUG || VERBOSE) {
            System.out.println(formatMessage("INFO ", ANSI_GREEN, ANSI_BG_GREEN, "ℹ️", message));
        }
    }

    /**
     * Success level logging - for positive outcomes
     */
    public void success(String message) {
        System.out.println(formatMessage("SUCCESS", ANSI_GREEN, ANSI_BG_GREEN, "✅", message));
    }

    /**
     * Warning level logging - for potential issues
     */
    public void warning(String message) {
        System.out.println(formatMessage("WARNING", ANSI_YELLOW, ANSI_BG_YELLOW, "⚠️", message));
    }

    /**
     * Error level logging with exception details
     */
    public void error(String message, Exception ex) {
        System.out.println(formatMessage("ERROR", ANSI_RED, ANSI_BG_RED, "❌", message));
        if (ex != null) {
            System.out.println(ANSI_RED + "    ↳ " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ANSI_RESET);
            if (DEBUG) {
                // Show full stack trace only in debug mode
                ex.printStackTrace();
            } else {
                // Show just the relevant stack trace elements
                StackTraceElement[] stack = ex.getStackTrace();
                for (int i = 0; i < Math.min(3, stack.length); i++) {
                    System.out.println(ANSI_DIM + "      at " + stack[i] + ANSI_RESET);
                }
                if (stack.length > 3) {
                    System.out.println(ANSI_DIM + "      ... " + (stack.length - 3) + " more" + ANSI_RESET);
                }
            }
        }
    }

    /**
     * Error level logging without exception
     */
    public void error(String message) {
        error(message, null);
    }

    /**
     * Critical error logging - for fatal issues
     */
    public void fatal(String message) {
        System.out.println(formatMessage("FATAL", ANSI_RED + ANSI_BOLD, ANSI_BG_RED, "💀", message));
    }

    /**
     * Progress logging - for showing operation progress
     */
    public void progress(String operation, int current, int total) {
        if (VERBOSE) {
            int percentage = (current * 100) / total;
            String progressBar = createProgressBar(percentage, 20);
            String message = String.format("%s %s [%d/%d] %d%%", 
                progressBar, operation, current, total, percentage);
            System.out.print("\r" + formatMessage("PROGRESS", ANSI_BLUE, ANSI_BG_BLUE, "⏳", message));
            if (current == total) {
                System.out.println(); // New line when complete
            }
        }
    }

    /**
     * Creates a visual progress bar
     */
    private String createProgressBar(int percentage, int length) {
        int filled = (percentage * length) / 100;
        StringBuilder bar = new StringBuilder();
        bar.append(ANSI_GREEN);
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        bar.append(ANSI_DIM);
        for (int i = filled; i < length; i++) {
            bar.append("░");
        }
        bar.append(ANSI_RESET);
        return bar.toString();
    }

    /**
     * Separator for grouping related log messages
     */
    public void separator(String title) {
        if (VERBOSE) {
            String line = "═".repeat(50);
            if (title != null && !title.isEmpty()) {
                System.out.println(ANSI_CYAN + ANSI_BOLD + "╔" + line + "╗" + ANSI_RESET);
                System.out.println(ANSI_CYAN + ANSI_BOLD + "║" + String.format(" %-48s ", title) + "║" + ANSI_RESET);
                System.out.println(ANSI_CYAN + ANSI_BOLD + "╚" + line + "╝" + ANSI_RESET);
            } else {
                System.out.println(ANSI_DIM + "─".repeat(60) + ANSI_RESET);
            }
        }
    }
}
