package com.vityazev_egor;

import java.io.IOException;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        NoDriver d = new NoDriver();
        d.loadUrl("file:///home/egor/Desktop/testB.html");
        Thread.sleep(1000);
        for (int i=0; i<10; i++){
            d.emulateClick(10, 10);
        }
        Thread.sleep(5000);
        d.loadUrl("https://google.com");
        Thread.sleep(2000);
        System.out.println("Got result "+d.getTitle());
        d.loadUrl("https://dev.cfcybernews.eu");
        Thread.sleep(10000);
        System.out.println("Got result "+d.getTitle());
        d.exit();
    }
}
