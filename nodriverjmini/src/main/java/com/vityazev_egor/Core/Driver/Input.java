package com.vityazev_egor.Core.Driver;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebElements.WebElement;

public class Input {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Input (NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(Input.class.getName());
    }

    public void emualteMouseMove(Integer x, Integer y){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genMouseMove(x, y));
    }

    // NOT WORKING | TESTING
    public void emulateMouseWheel(Integer deltaY, Integer x, Integer y){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genMouseWheel(deltaY, x, y));
    }

    public void emulateEndClick(){
        driver.getSocketClient().sendCommand(driver.getCmdProcessor().genKeyInput());
    }
    // NOT WORKING | TESTING

    public void emulateClick(Integer x, Integer y){
        String[] json = driver.getCmdProcessor().genMouseClick(x, y);
        driver.getSocketClient().sendCommand(json[0]);
        driver.getSocketClient().sendCommand(json[1]);
    }

    public void emulateClick(Double x, Double y){
        emulateClick(x.intValue(), y.intValue());
    }

    public void emulateClick(WebElement element){
        element.getPosition().ifPresentOrElse(
            position -> emulateClick(position.getX(), position.getY()), 
            () -> logger.error("Can't get element position")
        );
    }

    public void enterText(WebElement element, String text){
        element.getFocus();
        driver.getCmdProcessor().genTextInput(text).forEach(
            json-> driver.getSocketClient().sendAndWaitResult(1, json, 15)
        );
    }

    public void insertText(WebElement element, String text){
        element.getFocus();
        var json = driver.getCmdProcessor().genInsertText(text);
        driver.getSocketClient().sendCommand(json);
    }
}
