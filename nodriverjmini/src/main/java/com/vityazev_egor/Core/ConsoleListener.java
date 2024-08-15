package com.vityazev_egor.Core;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.vityazev_egor.NoDriver;

public class ConsoleListener implements Runnable{
    private final Process process;
    public ConsoleListener(Process p){
        this.process = p;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (!NoDriver.isInit) NoDriver.isInit = true;
            }
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
            
    }
    
}
