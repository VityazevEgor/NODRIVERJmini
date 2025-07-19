package com.vityazev_egor.Core.WebElements;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.LambdaWaitTask;
import org.apache.commons.imaging.Imaging;

import java.awt.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.Shared;
import com.vityazev_egor.Core.WaitTask;

public class WebElement {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CustomLogger logger = new CustomLogger(WebElement.class.getName());

    private String getPositionJs;
    private String isClickableJs;
    private String getContentJs;
    private String getTextJs;
    private String getSizeJs;
    private String isExistsJs;
    private String getValueJs;

    private final String elementJs;
    private final NoDriver driver;

    public WebElement(NoDriver driver, By by){
        this.elementJs = by.getJavaScript();
        this.driver = driver;
        initScripts();
    }

    public WebElement(NoDriver driver, String elementJs){
        this.elementJs = elementJs;
        this.driver = driver;
        initScripts();
    }

    private void initScripts(){
        this.getPositionJs = Shared.readResource("elementsJS/getPosition.js").get().replace("REPLACE_ME", elementJs);
        this.getSizeJs = Shared.readResource("elementsJS/getSize.js").get().replace("REPLACE_ME", elementJs);
        this.isClickableJs = Shared.readResource("elementsJS/isElementClickable.js").get().replace("REPLACE_ME", elementJs);
        this.isExistsJs = Shared.readResource("elementsJS/isElementExists.js").get().replace("REPLACE_ME", elementJs);
        this.getContentJs = elementJs + ".innerHTML";
        this.getTextJs = elementJs + ".innerText";
        this.getValueJs = elementJs + ".value";
    }

    /**
     * Checks whether the element exists in the DOM.
     *
     * @return {@code true} if the element exists, {@code false} otherwise.
     */
    public boolean isExists(){
        var result = driver.executeJSAndGetResult(isExistsJs);
        return result.map((jsResult) ->{
            try {
                return Boolean.parseBoolean(jsResult);
            } catch (Exception e) {
                return false;
            }
        }).orElse(false);
    }

    /**
     * Retrieves the position of the element on the page.
     * {@link Point} coordinates of center of element
     *
     * @return An {@link Optional} containing the {@link Point} if found, or an empty {@link Optional} if not found.
     */
    public Optional<Point> getPosition(){
        var result = driver.executeJSAndGetResult(getPositionJs);
        if (result.isEmpty()) return Optional.empty();

        String jsonResponse = result.get();
        if (jsonResponse.contains("not found")) return Optional.empty();

        try{
            JsonNode tree = mapper.readTree(jsonResponse);
            String xRaw = tree.get("x").asText();
            String yRaw = tree.get("y").asText();

            return Optional.of(new Point(Integer.parseInt(xRaw), Integer.parseInt(yRaw)));
        }
        catch (Exception ex){
            logger.error("Can't get position of element", ex);
            return Optional.empty();
        }
    }

    /**
     * Retrieves the size (width and height) of the element.
     *
     * @return An {@link Optional} containing the {@link Dimension} if found, or an empty {@link Optional} if not found.
     */
    public Optional<Dimension> getSize(){
        var result = driver.executeJSAndGetResult(getSizeJs);

        String jsonResponse = result.get();
        if (jsonResponse.contains("not found")) return Optional.empty();

        try{
            JsonNode tree = mapper.readTree(jsonResponse);
            String xRaw = tree.get("x").asText();
            String yRaw = tree.get("y").asText();

            return Optional.of(new Dimension(Integer.parseInt(xRaw), Integer.parseInt(yRaw)));
        }
        catch (Exception ex){
            logger.error("Can't get size of element", ex);
            return Optional.empty();
        }
    }

    /**
     * Sets focus on the element.
     */
    public void getFocus(){
        driver.executeJS(elementJs + ".focus()");
    }

    /**
     * Retrieves the inner HTML content of the element.
     *
     * @return An {@link Optional} containing the HTML content as a string, or an empty {@link Optional} if unavailable.
     */
    public Optional<String> getHTMLContent(){
        return driver.executeJSAndGetResult(getContentJs);
    }

    /**
     * Retrieves the visible text content of the element.
     *
     * @return An {@link Optional} containing the text content as a string, or an empty {@link Optional} if unavailable.
     */
    public Optional<String> getText(){
        return driver.executeJSAndGetResult(getTextJs);
    }

    /**
     * Retrieves the value of the element, typically for input fields.
     *
     * @return An {@link Optional} containing the value as a string, or an empty {@link Optional} if unavailable.
     */
    public Optional<String> getValue(){
        return driver.executeJSAndGetResult(getValueJs);
    }

    /**
     * Checks whether the element is clickable.
     *
     * @return {@code true} if the element is clickable, {@code false} otherwise.
     */
    public boolean isClickable(){
        var result = driver.executeJSAndGetResult(isClickableJs);
        if (result.isEmpty()) return false;

        try {
            return Boolean.parseBoolean(result.get());
        } catch (Exception ex) {
            logger.error("Can't check is element is clickable", ex);
            return false;
        }
    }

    /**
     * Retrieves the value of the specified attribute from the element.
     *
     * @param attributeName The name of the attribute to retrieve (e.g., "src", "alt", "href").
     * @return An {@link Optional} containing the attribute value as a string, or an empty {@link Optional} if unavailable.
     */
    public Optional<String> getAttribute(String attributeName){
        String getAttributeJs = elementJs + ".getAttribute('" + attributeName + "')";
        return driver.executeJSAndGetResult(getAttributeJs);
    }

    /**
     * Waits for the element to appear within the specified timeout.
     *
     * @param timeOutSeconds The maximum number of seconds to wait.
     * @param delayMilis     The delay in milliseconds between condition checks.
     * @throws Exception If the element does not appear within the timeout period.
     */
    public void waitToAppear(Integer timeOutSeconds, Integer delayMilis) throws Exception {
        var waitTask = new LambdaWaitTask(this::isExists);

        if (!waitTask.execute(timeOutSeconds, delayMilis))
            throw new Exception("Element not found: " + elementJs);
    }
}