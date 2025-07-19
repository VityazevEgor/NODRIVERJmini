package com.vityazev_egor;

import com.vityazev_egor.Core.WebElements.WebElement;
import org.junit.jupiter.api.Test;

import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

class ApplicationTest {

    @Test
    void testCFBypassXDO() throws IOException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        d.getXdo().calibrate();
        Boolean result = d.getNavigation().loadUrlAndBypassCFXDO("https://nopecha.com/demo/cloudflare", 5, 30);
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

    @Test
    void exampleUsage() {
        try {
            // Initialize a new NoDriver instance
            NoDriver driver = new NoDriver();

            // Load the URL "https://google.com" and wait for it to fully load
            driver.getNavigation().loadUrlAndWait("https://google.com", 10);

            // Find the textarea element with the specified CSS selector
            WebElement textArea = driver.findElement(By.cssSelector("textarea[maxlength='2048']"));

            // Wait for the textarea to appear on the page
            textArea.waitToAppear(5, 200);

            // Insert text "NODRIVERJmini" into the textarea
            driver.getInput().insertText(textArea, "NODRIVERJmini");

            // Pause for 100 milliseconds
            Shared.sleep(100);

            // Find all suggestion elements with the specified CSS selector
            List<WebElement> suggestions = driver.findElements(By.cssSelector("li[data-attrid='AutocompletePrediction']"));

            // Check if any suggestions were found
            if (suggestions.isEmpty())
                throw new Exception("Can't find suggestions");

            // Print each suggestion's text
            suggestions.forEach(suggestion -> suggestion.getText().ifPresent(System.out::println));

            // Emulate a click on the first suggestion
            driver.getInput().emulateClick(suggestions.getFirst());

            // Pause for 50 milliseconds
            Shared.sleep(50);

            // Wait for the navigation to fully load
            driver.getNavigation().waitFullLoad(5);

            // Capture a screenshot and save it as "search_results.png"
            driver.getMisc().captureScreenshot(Path.of("search_results.png"));

            // Exit the NoDriver instance
            driver.exit();
        } catch (IOException ex){
            System.err.println("Can't init chrome: " + ex.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Can't find element(s):" + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }
}
