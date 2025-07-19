package com.vityazev_egor.Core.Driver;

import java.util.ArrayList;
import java.util.List;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.CDPCommandBuilder;
import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.WebElements.WebElement;

public class Input {
    private final NoDriver driver;
    private final CustomLogger logger;

    public Input (NoDriver driver){
        this.driver = driver;
        this.logger = new CustomLogger(Input.class.getName());
    }

    /**
     * Emulates mouse movement to the specified coordinates.
     *
     * @param x The X coordinate to move the mouse to.
     * @param y The Y coordinate to move the mouse to.
     */
    public void emulateMouseMove(Integer x, Integer y){
        String command = CDPCommandBuilder.create("Input.dispatchMouseEvent")
            .addParam("type", "mouseMoved")
            .addParam("x", x)
            .addParam("y", y)
            .addParam("modifiers", 0)
            .addParam("button", "none")
            .addParam("buttons", 0)
            .addParam("pointerType", "mouse")
            .build();
        driver.getSocketClient().sendCommand(command);
    }

    // NOT WORKING | TESTING
    public void emulateMouseWheel(Integer deltaY, Integer x, Integer y){
        String command = CDPCommandBuilder.create("Input.dispatchMouseEvent")
            .addParam("type", "mouseWheel")
            .addParam("x", x)
            .addParam("y", y)
            .addParam("deltaX", 0)
            .addParam("deltaY", deltaY)
            .addParam("modifiers", 0)
            .addParam("button", "middle")
            .addParam("buttons", 0)
            .build();
        driver.getSocketClient().sendCommand(command);
    }

    public void emulateEndClick(){
        String keyDown = CDPCommandBuilder.create("Input.dispatchKeyEvent")
            .addParam("type", "keyDown")
            .addParam("key", "End")
            .addParam("code", "End")
            .addParam("keyCode", 35)
            .addParam("modifiers", 0)
            .addParam("autoRepeat", false)
            .addParam("isKeypad", false)
            .addParam("isSystemKey", true)
            .addParam("location", 0)
            .build();

        String keyUp = CDPCommandBuilder.create("Input.dispatchKeyEvent")
            .addParam("type", "keyUp")
            .addParam("key", "End")
            .addParam("code", "End")
            .addParam("keyCode", 35)
            .addParam("modifiers", 0)
            .addParam("autoRepeat", false)
            .addParam("isKeypad", false)
            .addParam("isSystemKey", true)
            .addParam("location", 0)
            .build();

        driver.getSocketClient().sendCommand(keyDown);
        driver.getSocketClient().sendCommand(keyUp);
    }
    // NOT WORKING | TESTING

    /**
     * Emulates a mouse click at the specified coordinates.
     *
     * @param x The X coordinate where the click should occur.
     * @param y The Y coordinate where the click should occur.
     */
    public void emulateClick(Integer x, Integer y){
        // Mouse pressed event
        String pressedCommand = CDPCommandBuilder.create("Input.dispatchMouseEvent")
            .addParam("type", "mousePressed")
            .addParam("x", x)
            .addParam("y", y)
            .addParam("button", "left")
            .addParam("buttons", 1)
            .addParam("clickCount", 1)
            .addParam("pointerType", "mouse")
            .build();

        // Mouse released event
        String releasedCommand = CDPCommandBuilder.create("Input.dispatchMouseEvent")
            .addParam("type", "mouseReleased")
            .addParam("x", x)
            .addParam("y", y)
            .addParam("button", "left")
            .addParam("buttons", 0)
            .addParam("pointerType", "mouse")
            .build();

        driver.getSocketClient().sendCommand(pressedCommand);
        driver.getSocketClient().sendCommand(releasedCommand);
    }

    /**
     * Emulates a mouse click at the specified coordinates.
     * Converts double values to integer before execution.
     *
     * @param x The X coordinate as a double.
     * @param y The Y coordinate as a double.
     */
    public void emulateClick(Double x, Double y){
        emulateClick(x.intValue(), y.intValue());
    }

    /**
     * Emulates a mouse click on the specified web element.
     * <p>
     * If the element's position is available, the click is performed at its coordinates.
     * Otherwise, an error is logged.
     *
     * @param element The WebElement to click.
     */
    public void emulateClick(WebElement element){
        element.getPosition().ifPresentOrElse(
            position -> emulateClick(position.getX(), position.getY()), 
            () -> logger.error("Can't get element position")
        );
    }

    /**
     * Enters text into the specified web element by simulating keypresses.
     * The element is focused before inputting text.
     *
     * @param element The WebElement to input text into.
     * @param text The text to be entered.
     */
    public void enterText(WebElement element, String text){
        element.getFocus();
        for (char c : text.toCharArray()) {
            String command = CDPCommandBuilder.create("Input.dispatchKeyEvent")
                .addParam("type", "char")
                .addParam("text", String.valueOf(c))
                .build();
            driver.getSocketClient().sendAndWaitResult(1, command, 15);
        }
    }

    /**
     * Inserts text into the specified web element without simulating keypresses.
     * The element is focused before inserting text.
     *
     * @param element The WebElement to insert text into.
     * @param text The text to be inserted.
     */
    public void insertText(WebElement element, String text){
        element.getFocus();
        String command = CDPCommandBuilder.create("Input.insertText")
            .addParam("text", text)
            .build();
        driver.getSocketClient().sendCommand(command);
    }
}
