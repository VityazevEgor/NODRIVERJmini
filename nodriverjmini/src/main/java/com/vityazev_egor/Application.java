package com.vityazev_egor;

import java.io.IOException;

import java.util.*;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        testLoadAndWait();
    }

    public static void testMouseMove() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.loadUrlAndWait("file:///home/egor/Desktop/mouse.html",10);
        d.emualteMouseMove(10, 10);
        Thread.sleep(5000);
        d.exit();
    }

    public static void testLoadAndWait() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        Boolean result = d.loadUrlAndWait("https://bing.com", 5);
        if (result){
            System.out.println("Loaded site!");
        }
        else{
            System.err.println("Can't load site");
        }
        result = d.loadUrlAndBypassCF("https://forum.cfcybernews.eu", 5, 20);
        if (result){
            System.out.println("Bypassed CloudFlare");
        }
        else{
            System.err.println("Can't bypass CloudFlare");
        }
        //Thread.sleep(10000);
        d.exit();
    }

    public void majorTest() throws InterruptedException, IOException{
        NoDriver d = new NoDriver();
        d.loadUrl("file:///home/egor/Desktop/testIN.html");
        Thread.sleep(1000);
        for (int i=0; i<10; i++){
            //d.emulateClick(10, 10);
        }
        d.enterText("Как какать?", "testinput");
        Thread.sleep(1000);
        d.loadUrl("file:///home/egor/Desktop/testB.html");
        Thread.sleep(2000);
        d.emulateKey();
        Thread.sleep(1000);
        d.loadUrl("https://bing.com");
        Thread.sleep(5000);
        d.enterText("Что такое майнкрафт?", "sb_form_q");
        d.testElementLocation("sb_form_q");
        Thread.sleep(10000);
        d.exit();
        System.exit(0);
        d.loadUrl("https://google.com");
        Thread.sleep(2000);
        System.out.println("Got result "+d.getTitle());
        d.loadUrl("https://dev.cfcybernews.eu");
        Thread.sleep(10000);
        System.out.println("Got result "+d.getTitle());
        d.exit();
    }
}
