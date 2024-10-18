package com.vityazev_egor.Core.Driver;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import java.util.*;

public class Navigation {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Navigation(NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(Navigation.class.getName());
    }

    public void loadUrl(String url){
        String json = driver.getCmdProcessor().genLoadUrl(url);
        driver.getSocketClient().sendCommand(json);
    }

    public Boolean loadUrlAndWait(String url, Integer timeOutSeconds){
        loadUrl(url);
        Shared.sleep(500); // we need give some time for browser to start loading of page
        var task = new WaitTask() {

            @Override
            public Boolean condition() {
                Optional<String> response = driver.getSocketClient().sendAndWaitResult(2, driver.getCmdProcessor().genExecuteJs("document.readyState"));
                if (!response.isPresent()) return false;
                String jsResult = driver.getCmdProcessor().getJsResult(response.get());

                return jsResult.equals("complete");
            }
            
        };
        return task.execute(timeOutSeconds, 500);
    }

    // works even if u use proxy
    public Boolean loadUrlAndBypassCFXDO(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
        Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
        if (!loadResult) return false;

        var html = driver.getHtml();
        if (!html.isPresent()) return false;

        if (html.get().contains("ray-id")){
            logger.warning("Detected CloudFlare");
            var task = new WaitTask() {

                @Override
                public Boolean condition() {
                    var currentHtml = driver.getHtml();
                    if (!currentHtml.isPresent()) return false;
                    if (currentHtml.get().contains("ray-id")){
                        var spacer = driver.findElement(By.cssSelector("div[style*=\"display: grid;\"]"));
                        var spacerPoint = driver.getElementPosition(spacer);
                        var spacerSize = driver.getElementSize(spacer);
                        if (!spacerPoint.isPresent() || !spacerSize.isPresent()){
                            logger.info("Spacer div is still loading...");
                            return false;
                        }
                        Integer realX = spacerPoint.get().x - spacerSize.get().width/2 + 30;
                        driver.getXdo().click(realX, spacerPoint.get().y);
                        return false;
                    }
                    else{
                        return true;
                    }
                }
                
            };
            return task.execute(cfBypassTimeOutSeconds, 1000);
        }
        else{
            logger.warning("There is no CloudFlare");
            return true;
        }
    }
    
    // works only if ur IP is not related to hsoting IPs
    // public Boolean loadUrlAndBypassCFCDP(String url, Integer urlLoadTimeOutSeconds, Integer cfBypassTimeOutSeconds){
    //     Boolean loadResult = loadUrlAndWait(url, urlLoadTimeOutSeconds);
    //     if (!loadResult) return false;

    //     var html = getHtml();
    //     if (!html.isPresent()) return false;

    //     if (html.get().contains("ray-id")){
    //         logger.warning("Detected CloudFlare");
    //         var task = new WaitTask(){

    //             @Override
    //             public Boolean condition() {
    //                 var currentHtml = getHtml();
    //                 if (!currentHtml.isPresent()) return false;
    //                 var currentPageTime = getCurrentPageTime();
    //                 if (!currentPageTime.isPresent()) return false;
    //                 if (currentHtml.get().contains("ray-id")){
    //                     socketClient.sendCommand(cmdProcessor.genMouseClick(190, 287, currentPageTime.get()));
    //                     return false;
    //                 }
    //                 else{
    //                     return true;
    //                 }
    //             }
                
    //         };
    //         return task.execute(cfBypassTimeOutSeconds, 1000);
    //     }
    //     else{
    //         logger.warning("There is no CloudFlare");
    //         return true;
    //     }
    // }


}
