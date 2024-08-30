package com.vityazev_egor.Core.WebElements;

import java.awt.Point;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.NoDriver;


// TODO реализовать поиск элементов внутри shadow root элемента
public class WebElement {
    private final ObjectMapper mapper = new ObjectMapper();

    private String getPositionJs;
    private String isClickableJs;
    private By by;

    public WebElement(By by){
        this.by = by;

        this.getPositionJs = String.format(
            """
            function getElementPosition() {
                var element = %s;
                if (element) {
                    var rect = element.getBoundingClientRect();
                    var position = {
                        x: Math.round(rect.left + rect.width / 2),
                        y: Math.round(rect.top + rect.height / 2)
                    };
                    return JSON.stringify(position);
                } else {
                    return JSON.stringify({ error: "Element not found" });
                }
            }
            getElementPosition();
            """
            , this.by.getJavaScript());

        this.isClickableJs = String.format(
            """
            function isElementClickable() {
                var element = %s;
                if (!element) {
                    return false;
                }
                var style = window.getComputedStyle(element);
                var isVisible = style.display !== 'none' && style.visibility !== 'hidden' && style.opacity > 0;
                var rect = element.getBoundingClientRect();
                var isUnderOtherElement = document.elementFromPoint(rect.left + 1, rect.top + 1) !== element;
                var isEnabled = !element.disabled;
                return isVisible && !isUnderOtherElement && isEnabled;
            }
            isElementClickable();
            """
            , this.by.getJavaScript());
    }

    public Optional<Point> getPosition(NoDriver driver){
        var result = driver.getJSResult(getPositionJs);
        if (!result.isPresent()) return Optional.empty();

        String jsonResponse = result.get();
        if (jsonResponse.contains("not found")) return Optional.empty();

        try{
            JsonNode tree = mapper.readTree(jsonResponse);
            String xRaw = tree.get("x").asText();
            String yRaw = tree.get("y").asText();

            return Optional.of(new Point(Integer.parseInt(xRaw), Integer.parseInt(yRaw)));
        }
        catch (Exception ex){
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    public void getFocus(NoDriver driver){
        driver.executeJS(by.getJavaScript() + ".focus()");
    }

    public Boolean isClickable(NoDriver driver){
        var result = driver.getJSResult(isClickableJs);
        if (!result.isPresent()) return false;

        try {
            return Boolean.parseBoolean(result.get());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}