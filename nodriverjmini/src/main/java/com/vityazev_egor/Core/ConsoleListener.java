package com.vityazev_egor.Core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vityazev_egor.NoDriver;

public class ConsoleListener implements Runnable{
    private Process process = null;
    private Boolean logMessages = false;
    private List<String> consoleMessages = new ArrayList<>();

    public ConsoleListener(Process p){
        this.process = p;
    }

    public ConsoleListener(String consoleCommand){
        List<String> command = Arrays.asList(consoleCommand.split(" "));
        ProcessBuilder pBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        try {
            process = pBuilder.start();
            logMessages = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't start process in 'ConsoleListener(String)'");
        }
    }

    @Override
    public void run() {
        if (process == null) return;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (logMessages) consoleMessages.add(line);
                if (!NoDriver.isInit) NoDriver.isInit = true;
            }
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
            
    }

    public long getPid(){
        return process.pid();
    }

    public List<String> getConsoleMessages(){
        return consoleMessages;
    }
    
}
