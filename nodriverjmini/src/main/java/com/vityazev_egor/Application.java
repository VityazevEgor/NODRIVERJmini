package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WebElements.By;

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
            driver.getInput().emulateClick(element);
            driver.getInput().enterText(element, "Как какать?");
            driver.getInput().emulateClick(driver.findElement(By.id("search_icon")));
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
        d.getInput().emualteMouseMove(10, 10);
        Thread.sleep(5000);
        d.exit();
    }
    @SuppressWarnings("unused")
    private static void testCalibrateXDO() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.getXdo().calibrate();
        waitEnter();
        d.exit();
    }

    @SuppressWarnings("unused")
    private static void testCloudFlareBypass() throws IOException, InterruptedException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        d.clearCookies();
        d.getXdo().calibrate();
        var result = d.loadUrlAndBypassCFXDO("https://forum.cfcybernews.eu", 5, 10);
        if (result){
            System.out.println("Bypassed CloudFlare");
        }
        else{
            System.err.println("Can't bypass CloudFlare");
            d.captureScreenshot(Path.of("cf.png"));
        }
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        d.exit();
        sc.close();
    }
}
