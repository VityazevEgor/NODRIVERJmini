package com.vityazev_egor.Core.Driver;

import java.util.List;

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

    public void emulateClick(Integer x, Integer y){
        String[] json = driver.getCmdProcessor().genMouseClick(x, y);
        driver.getSocketClient().sendCommand(json[0]);
        driver.getSocketClient().sendCommand(json[1]);
    }

    public void emulateClick(Double x, Double y){
        emulateClick(x.intValue(), y.intValue());
    }

    public void emulateClick(WebElement element){
        var position = element.getPosition(driver);
        if (!position.isPresent()){
            logger.warning("Can't get element position to click");
            return;
        }

        emulateClick(position.get().getX(), position.get().getY());
    }

    public void enterText(String text, String elementId){
        List<String> jsons = driver.getCmdProcessor().genTextInput(text, elementId);
        for (String json : jsons){
            driver.getSocketClient().sendCommand(json);
        }
    }

    public void enterText(WebElement element, String text){
        element.getFocus(driver);
        var list = driver.getCmdProcessor().genTextInput(text);
        driver.getSocketClient().sendCommand(list);
    }
}
