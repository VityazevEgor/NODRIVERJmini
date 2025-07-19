package com.vityazev_egor.Core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommandRunner {
    private static final CustomLogger logger = new CustomLogger(CommandRunner.class.getName());

    /**
     * Executes a system command and returns the output lines.
     * Properly handles commands with quoted arguments.
     *
     * @param command The command to execute
     * @return List of output lines from the command
     * @throws RuntimeException if command execution fails
     */
    public static List<String> executeCommand(String command) {
        try {
            // Use ProcessBuilder with proper command parsing
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // For Linux/Unix systems, use shell to properly handle quotes
            if (isWindows()) {
                processBuilder.command("cmd", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            List<String> output = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warning("Command exited with code: " + exitCode + " for command: " + command);
            }
            
            return output;
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing command: " + command, e);
            throw new RuntimeException("Failed to execute command: " + command, e);
        }
    }

    /**
     * Determines if the current operating system is Windows.
     *
     * @return {@code true} if running on Windows, {@code false} otherwise.
     */
    private static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("win");
    }
}