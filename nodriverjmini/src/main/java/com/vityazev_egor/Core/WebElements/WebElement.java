package com.vityazev_egor.Core.WebElements;

import java.awt.Point;
import java.util.Optional;
import java.awt.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.Shared;

public class WebElement {
    private final ObjectMapper mapper = new ObjectMapper();

    private final String getPositionJs;
    private final String isClickableJs;
    private final String getContentJs;
    private final String getSizeJs;
    private By by;

    public WebElement(By by){
        this.by = by;
        this.getPositionJs = Shared.readResource("elementsJS/getPosition.js").get().replace("REPLACE_ME", this.by.getJavaScript());
        this.getSizeJs = Shared.readResource("elementsJS/getSize.js").get().replace("REPLACE_ME", this.by.getJavaScript());
        this.isClickableJs = Shared.readResource("elementsJS/isElementClickable.js").get().replace("REPLACE_ME", this.by.getJavaScript());
        this.getContentJs = this.by.getJavaScript() + ".innerHTML";
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

    public Optional<Dimension> getSize(NoDriver driver){
        var result = driver.getJSResult(getSizeJs);

        String jsonResponse = result.get();
        if (jsonResponse.contains("not found")) return Optional.empty();

        try{
            JsonNode tree = mapper.readTree(jsonResponse);
            String xRaw = tree.get("x").asText();
            String yRaw = tree.get("y").asText();

            return Optional.of(new Dimension(Integer.parseInt(xRaw), Integer.parseInt(yRaw)));
        }
        catch (Exception ex){
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    public void getFocus(NoDriver driver){
        driver.executeJS(by.getJavaScript() + ".focus()");
    }

    public Optional<String> getContent(NoDriver driver){
        return driver.getJSResult(getContentJs);
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