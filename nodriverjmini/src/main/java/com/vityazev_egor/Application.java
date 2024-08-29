package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;
import java.nio.file.*;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        testSreenShot();
    }

    public static void testSreenShot() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.loadUrlAndWait("https://ya.ru", 10);
        var image = d.captureScreenshot();
        Thread.sleep(2000);
        ImageIO.write(image.get(), "png", Path.of("test.png").toFile());
        d.exit();
    }

    public static void testMouseMove() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.loadUrlAndWait("file:///home/egor/Desktop/mouse.html",10);
        d.emualteMouseMove(10, 10);
        Thread.sleep(5000);
        d.exit();
    }

    public static void testViewPort(){

    }

    @SuppressWarnings("unused")
    private static void testCloudFlareBypass() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        var result = d.loadUrlAndBypassCFXDO("https://forum.cfcybernews.eu", 5, 20);
        if (result){
            System.out.println("Bypassed CloudFlare");
        }
        else{
            System.err.println("Can't bypass CloudFlare");
        }
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        d.exit();
        sc.close();
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
