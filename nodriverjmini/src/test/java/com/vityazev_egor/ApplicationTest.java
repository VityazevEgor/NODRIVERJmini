package com.vityazev_egor;

import org.junit.jupiter.api.Test;

import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

class ApplicationTest {

    @Test
    void testCFBypassXDO() throws IOException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        d.getXdo().calibrate();
        Boolean result = d.getNavigation().loadUrlAndBypassCFXDO("https://dstatlove.ink/hit", 5, 30);
        d.exit();
        assertTrue(result);
    }

    @Test
    void testCFBypassCDP()throws IOException{
        NoDriver d = new NoDriver();
        Boolean result = d.getNavigation().loadUrlAndBypassCFCDP("https://nopecha.com/demo/cloudflare", 10, 30);
        d.exit();
        assertTrue(result);
    }

    @Test
    void testAntiBot() throws IOException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        Boolean result = d.getNavigation().loadUrlAndWait("https://bot.sannysoft.com/", 10);
        d.getMisc().captureScreenshot(Paths.get("antibot.png"));
        d.exit();
        assertTrue(result);
    }

    @Test
    void testAntiBot2() throws IOException, InterruptedException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        Boolean result = d.getNavigation().loadUrlAndWait("https://www.browserscan.net/bot-detection", 10);
        Thread.sleep(2000);
        d.getMisc().captureScreenshot(Paths.get("antibot.png"));
        d.exit();
        assertTrue(result);
    }

    @Test
    void testHtmlRendering()throws IOException{
        NoDriver d = new NoDriver();
        var result = d.getMisc().htmlToImage(Optional.of("<h1>Kek!</h1>"));
        d.exit();
        assertTrue(result.isPresent());
    }

    @Test
    void testScrollDown() throws IOException, InterruptedException{
        NoDriver driver = new NoDriver("127.0.0.1:2080");
        driver.getNavigation().loadUrlAndWait("https://copilot.microsoft.com/", 10);
        Thread.sleep(5000);
        driver.getMisc().setBypassCDP(true);

        // Ожидаем и нажимаем на первую кнопку "Sign in"
        var signInButton = driver.findElement(By.cssSelector("button[title=\"Sign in\"]"));
        if(signInButton.isExists()){
            System.out.print("Button exist");
            signInButton.testScreenshot();
        }

        var base64Div = driver.findElement(By.id("base64image"));
        WaitTask waitTask = new WaitTask() {
            @Override
            public Boolean condition() {
                return base64Div.isExists() && base64Div.getText().filter(text -> text.length() > 4).isPresent();
            }
        };

        var waitResult = waitTask.execute(5,100);
        System.out.println(base64Div.getText());
        driver.exit();
        assertTrue(waitResult);
    }

    @Test
    void testViewPort() throws IOException{
        NoDriver d = new NoDriver();
        var dm = d.getViewPortSize();
        System.out.println("Dimesion viewport= "+dm.get().getWidth() + ":"+dm.get().getHeight());
        d.exit();
        assertTrue(dm.isPresent());
    }

    @Test
    void testLoadAndWait() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        Boolean result = d.getNavigation().loadUrlAndWait("https://bing.com", 10);
        Boolean result2 = d.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        d.exit();
        assertTrue(result);
        assertTrue(result2);
    }

    @Test
    void testTextEntering() throws IOException, InterruptedException
    {
        NoDriver d = new NoDriver();
        d.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        var input = d.findElement(By.id("text"));
        d.getInput().enterText(input, "Test");
        Thread.sleep(2000);
        d.exit();
    }

    @Test
    void testInsertText() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        d.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        var input = d.findElement(By.id("text"));
        d.getInput().insertText(input, "Test");
        Thread.sleep(5000);
        d.exit();
    }

    @Test
    void testMultiTabs() throws IOException{
        NoDriver firstTab = new NoDriver();
        firstTab.getXdo().calibrate();
        firstTab.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        System.out.println("Loaded yandex");

        NoDriver secondTab = new NoDriver();
        secondTab.getNavigation().loadUrlAndWait("https://bing.com", 10);
        System.out.println("Loaded bing");

        firstTab.exit();
        System.out.println("Closed first tab");
        secondTab.exit();
        System.out.println("Closed second tab");
    }

    @Test
    void testJs() throws IOException{
        NoDriver d = new NoDriver();
        var title = d.getTitle();
        d.exit();
        assertTrue(title.isPresent());
        System.out.println(title.get());
    }

    @Test
    void testGetCurrentUrl() throws IOException{
        NoDriver d = new NoDriver();
        d.getNavigation().loadUrlAndWait("https://google.com", 10);
        var currentUrl = d.getCurrentUrl();
        d.exit();
        assertTrue(currentUrl.isPresent());
        assertTrue(currentUrl.get().contains("google.com"));
        System.out.println("Current URL: " + currentUrl.get());
    }
}
