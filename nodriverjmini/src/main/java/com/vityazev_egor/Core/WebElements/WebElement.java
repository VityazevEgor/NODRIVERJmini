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

    private String getPositionJs;
    private String isClickableJs;
    private String getContentJs;
    private String getTextJs;
    private String getSizeJs;
    private String isExistsJs;

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
        this.getTextJs = elementJs + ".textContent";
    }

    public Boolean isExists(){
        var result = driver.executeJSAndGetResult(isExistsJs);
        return result.map((jsResult) ->{
            try {
                return Boolean.parseBoolean(jsResult);
            } catch (Exception e) {
                return false;
            }
        }).orElse(false);
    }

    public Optional<Point> getPosition(){
        var result = driver.executeJSAndGetResult(getPositionJs);
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
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    public void getFocus(){
        driver.executeJS(elementJs + ".focus()");
    }

    public Optional<String> getHTMLContent(){
        return driver.executeJSAndGetResult(getContentJs);
    }

    public Optional<String> getText(){
        return driver.executeJSAndGetResult(getTextJs);
    }

    public Boolean isClickable(){
        var result = driver.executeJSAndGetResult(isClickableJs);
        if (!result.isPresent()) return false;

        try {
            return Boolean.parseBoolean(result.get());
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}