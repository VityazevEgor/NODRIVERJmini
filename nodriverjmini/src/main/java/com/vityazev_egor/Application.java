package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        testCloudFlareBypass();
    }

    public static void exampleCopilotAuth() throws IOException{
        NoDriver driver = new NoDriver("127.0.0.1:2080");
        System.out.println("Auth result = " + copilotAuth(driver));
        waitEnter();
        driver.exit();
    }

    private static Boolean copilotAuth(NoDriver driver) {
        driver.getNavigation().loadUrlAndWait("https://copilot.microsoft.com/", 10);

        // Ожидаем и нажимаем на первую кнопку "Sign in"
        var signInButton = driver.findElement(By.cssSelector("button[title=\"Sign in\"]"));
        var waitForSignInButton = new WaitTask() {
            @Override
            public Boolean condition() {
                return signInButton.isExists(driver);
            }
        };
        if (!waitForSignInButton.execute(5, 400)) {
            System.out.println("Can't find sign in button");
            return false;
        }
        driver.getInput().emulateClick(signInButton);

        // Проверяем наличие второй кнопки "Sign in" после раскрытия меню
        var signInButtons = driver.findElements(By.cssSelector("button[title=\"Sign in\"]"));
        if (signInButtons.size() < 2) {
            System.out.println("There are less than 2 'Sign in' buttons - " + signInButtons.size());
            return false;
        }
        driver.getInput().emulateClick(signInButtons.get(1));

        // Ожидаем появления поля для ввода логина
        var loginInput = driver.findElement(By.name("loginfmt"));
        var waitForLoginInput = new WaitTask() {
            @Override
            public Boolean condition() {
                return loginInput.isExists(driver);
            }
        };
        if (!waitForLoginInput.execute(5, 400)) {
            System.out.println("Can't find login input");
            return false;
        }

        // Вводим email и нажимаем кнопку "Далее"
        driver.getInput().enterText(loginInput, "test@gmail.com");
        var loginButton = driver.findElement(By.id("idSIButton9"));
        if (loginButton.isExists(driver)) {
            driver.getInput().emulateClick(loginButton);
        }

        return true;
    }
 

    public static void testMultiElements() throws IOException{
        NoDriver driver = new NoDriver();
        driver.getXdo().calibrate();
        driver.getNavigation().loadUrlAndWait("file:///home/egor/Desktop/test.html", 2);
        System.out.println(driver.getHtml());
        var elements = driver.findElements(By.cssSelector("[data-type=\"searchable\"]"));
        if (elements.size()>0){
            elements.get(elements.size()-1).getPosition(driver).map(pos ->{
                driver.getXdo().click(pos.getX(), pos.getY());
                return true;
            });
        }
        waitEnter();
        driver.exit();
    }

    public static void testProxy() throws IOException, InterruptedException{
        NoDriver driver = new NoDriver("127.0.0.1:2080");
        driver.getNavigation().loadUrlAndWait("https://2ip.ru", 10);
        waitEnter();
        driver.exit();
    }

    public static void testWebElemts() throws IOException, InterruptedException{
        NoDriver driver = new NoDriver();
        driver.getNavigation().loadUrlAndWait("https://bing.com", 10);
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
        System.out.println("Waiting for input");
        var sc = new Scanner(System.in);
        sc.nextLine();
        sc.close();
    }

    public static void testSreenShot() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        var image = d.getMisc().captureScreenshot();
        //Thread.sleep(2000);
        ImageIO.write(image.get(), "png", Path.of("test.png").toFile());
        d.exit();
    }

    public static void testMouseMove() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.getNavigation().loadUrlAndWait("file:///home/egor/Desktop/mouse.html",10);
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
        d.getMisc().clearCookies();
        d.getXdo().calibrate();
        var result = d.getNavigation().loadUrlAndBypassCFXDO("https://forum.cfcybernews.eu", 5, 30);
        if (result){
            System.out.println("Bypassed CloudFlare");
        }
        else{
            System.err.println("Can't bypass CloudFlare");
            d.getMisc().captureScreenshot(Path.of("cf.png"));
        }
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        d.exit();
        sc.close();
    }
}
