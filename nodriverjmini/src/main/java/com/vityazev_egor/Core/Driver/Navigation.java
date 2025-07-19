package com.vityazev_egor.Core.Driver;

import java.util.function.BiConsumer;

import com.vityazev_egor.Core.LambdaWaitTask;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CDPCommandBuilder;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WebElements.By;

public class Navigation {
    private final NoDriver driver;
    private final CustomLogger logger;
    private final LambdaWaitTask waitFullLoadTask;

    public Navigation(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(Navigation.class.getName());
        waitFullLoadTask = new LambdaWaitTask(() ->
                driver.executeJSAndGetResult("document.readyState")
                .map(result -> result.equalsIgnoreCase("complete"))
                .orElse(false)
        );
    }

    /**
     * Loads the specified URL in the tab.
     *
     * @param url The URL to be loaded.
     */
    public void loadUrl(String url){
        String command = CDPCommandBuilder.create("Page.navigate")
            .addParam("url", url)
            .build();
        driver.getSocketClient().sendCommand(command);
    }

    public void goBack(){
        driver.executeJS("window.history.back()");
    }

    public void goForward(){
        driver.executeJS("window.history.forward()");
    }

    /**
     * Loads a URL in the browser and waits for the page to fully load.
     *
     * @param url            The URL to be loaded.
     * @param timeOutSeconds The maximum time in seconds to wait for the page to load.
     * @return {@code true} if the page loaded successfully within the timeout, otherwise {@code false}.
     */
    public boolean loadUrlAndWait(String url, Integer timeOutSeconds){
        loadUrl(url);
        Shared.sleep(500); // we need give some time for browser to start loading of page
        return waitFullLoadTask.execute(timeOutSeconds, 500);
    }

    /**
     * Waits for the current page to fully load.
     *
     * @param timeOutSeconds The maximum time in seconds to wait for the page to load.
     * @return {@code true} if the page loaded successfully within the timeout, otherwise {@code false}.
     */
    public boolean waitFullLoad(Integer timeOutSeconds){
        return waitFullLoadTask.execute(timeOutSeconds, 500);
    }

    /**
     * Loads a URL and attempts to bypass the Cloudflare challenge if detected.
     *
     * @param url                    The URL to be loaded. If {@code null}, no initial page load is performed.
     * @param urlLoadTimeOutSeconds   The timeout for the initial page load.
     * @param cfBypassTimeOutSeconds  The timeout for the Cloudflare bypass process.
     * @param clickAction             A {@link BiConsumer} responsible for emulating a click on the Cloudflare challenge element.
     * @return {@code true} if the page loaded successfully and Cloudflare was bypassed (if present), otherwise {@code false}.
     */
    public boolean loadUrlAndBypassCF(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds, BiConsumer<Integer, Integer> clickAction) {
        if (url != null){
            boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
            if (!loadResult) return false;
        }

        try {
            var html = driver.getHtml().orElseThrow(() -> new Exception("Can't get html of page"));
            if (!html.contains("ray-id"))
                return true;
        }
        catch (Exception ex){
            logger.error(ex.getMessage(), ex);
        }
        
        logger.warning("Detected CloudFlare");
        var task = new LambdaWaitTask(()->{
           try{
               var currentHtml = driver.getHtml().orElseThrow(() -> new Exception("Can't get html of page"));
               var currentTitle = driver.getTitle().orElseThrow(() -> new Exception("Can't get title of page"));
               if (!currentHtml.contains("ray-id") && !currentTitle.contains("Just a moment"))
                   return true;

               var spacer = driver.findElement(By.cssSelector("div[style*=\"display: grid;\"]"));
               var spacerPoint = spacer.getPosition().orElseThrow(() -> new Exception("Can't get spacer position"));
               var spacerSize = spacer.getSize().orElseThrow(() -> new Exception("Can't get spacer size"));
               Integer realX = spacerPoint.x - spacerSize.width / 2 + 30;
               clickAction.accept(realX, spacerPoint.y);
               return false;
           }
           catch (Exception ex){
               logger.error(ex.getMessage());
               return false;
           }
        });
        return task.execute(cfBypassTimeOutSeconds, 1000);
    }

    /**
     * Loads a URL and attempts to bypass the Cloudflare challenge using xdotool.
     * This method works even if a proxy is used.
     *
     * @param url                    The URL to be loaded.
     * @param urlLoadTimeOutSeconds   The timeout for the initial page load.
     * @param cfBypassTimeOutSeconds  The timeout for the Cloudflare bypass process.
     * @return {@code true} if the page loaded successfully and Cloudflare was bypassed (if present), otherwise {@code false}.
     */
    public boolean loadUrlAndBypassCFXDO(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds) {
        return loadUrlAndBypassCF(url, urlLoadTimeOutSeconds, cfBypassTimeOutSeconds, driver.getXdo()::click);
    }

    /**
     * Loads a URL and attempts to bypass the Cloudflare challenge using Chrome DevTools Protocol (CDP).
     * This method works only if the IP is considered "clean."
     *
     * @param url                    The URL to be loaded.
     * @param urlLoadTimeOutSeconds   The timeout for the initial page load.
     * @param cfBypassTimeOutSeconds  The timeout for the Cloudflare bypass process.
     * @return {@code true} if the page loaded successfully and Cloudflare was bypassed (if present), otherwise {@code false}.
     */
    public boolean loadUrlAndBypassCFCDP(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds) {
        return loadUrlAndBypassCF(url, urlLoadTimeOutSeconds, cfBypassTimeOutSeconds, driver.getInput()::emulateClick);
    }

}
