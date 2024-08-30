package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WebElements.By;

import java.nio.file.*;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        testCloudFlareBypass();
    }

    public static void testProxy() throws IOException, InterruptedException{
        NoDriver driver = new NoDriver("127.0.0.1:2080");
        driver.loadUrlAndWait("https://2ip.ru", 10);
        waitEnter();
        driver.exit();
    }

    public static void testWebElemts() throws IOException, InterruptedException{
        NoDriver driver = new NoDriver();
        driver.loadUrlAndWait("https://bing.com", 10);
        var element = driver.findElement(By.id("sb_form_q"));
        if(element.isClickable(driver)){
            var elementPosition = element.getPosition(driver);
            if (elementPosition.isPresent()){
                System.out.println(elementPosition.get().getX());
                System.out.println(elementPosition.get().getY());
            }
            driver.emulateClick(element);
            driver.enterText(element, "Как какать?");
            driver.emulateClick(driver.findElement(By.id("search_icon")));
        }
        waitEnter();
        driver.exit();
    }

    public static void waitEnter(){
        var sc = new Scanner(System.in);
        sc.nextLine();
        sc.close();
    }

    public static void testSreenShot() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.loadUrlAndWait("https://ya.ru", 10);
        var image = d.captureScreenshot();
        //Thread.sleep(2000);
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
        d.clearCookies();
        var result = d.loadUrlAndBypassCFCDP("https://forum.cfcybernews.eu", 5, 20);
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
