package com.vityazev_egor.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommandsProcessor {
    private Integer id = 1;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public final String getElementLocation = """
            function getElementPositionById(id) {
                var element = document.getElementById(id);
                if (element) {
                    var rect = element.getBoundingClientRect();
                    var position = {
                        top: rect.top,
                        left: rect.left,
                        bottom: rect.bottom,
                        right: rect.right
                    };
                    return JSON.stringify(position);
                } else {
                    return JSON.stringify({ error: "Element not found" });
                }
            }
            getElementPositionById("ELEMENT_ID");
            """;

    private ObjectNode buildBase(String method, ObjectNode params){
        ObjectNode request = objectMapper.createObjectNode();
        request.put("id", id);
        request.put("method", method);
        if (params != null) request.set("params", params);
        id++;

        return request;
    }

    public Integer getIdOfLastMessage(){
        if (id>1){
            return id - 1;
        }
        else{
            return 1;
        }
    }

    private String serializeNode(ObjectNode node){
        String json = null;
        try {
            json = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            // it should always fork so i going to ignore errors
            e.printStackTrace();
        }

        return json;
    }

    @Nullable
    public String genLoadUrl(String url){
        ObjectNode params = objectMapper.createObjectNode();
        params.put("url", url);

        ObjectNode request = buildBase("Page.navigate", params);
        String json = serializeNode(request);
    
        return json;
    }

    public String genExecuteJs(String expression){
        ObjectNode params = objectMapper.createObjectNode();
        params.put("expression", expression);

        ObjectNode request = buildBase("Runtime.evaluate", params);
        String json = serializeNode(request);
        
        return json;
    }

    public String[] genMouseClick(int x, int y, Double currentPageTime) {

        // Создаем событие mousePressed
        ObjectNode paramsPressed = objectMapper.createObjectNode();
        paramsPressed.put("type", "mousePressed");
        paramsPressed.put("x", x);
        paramsPressed.put("y", y);
        paramsPressed.put("button", "left");
        paramsPressed.put("buttons", 1); // Левую кнопку мыши
        paramsPressed.put("clickCount", 1); // Один клик
        paramsPressed.put("pointerType", "mouse");
        if (currentPageTime != null){
            paramsPressed.put("timestamp", currentPageTime);
        }

        ObjectNode requestPressed = buildBase("Input.dispatchMouseEvent", paramsPressed);
        String jsonPressed = serializeNode(requestPressed);

        // Создаем событие mouseReleased
        ObjectNode paramsReleased = objectMapper.createObjectNode();
        paramsReleased.put("type", "mouseReleased");
        paramsReleased.put("x", x);
        paramsReleased.put("y", y);
        paramsReleased.put("button", "left");
        paramsReleased.put("buttons", 0); // Кнопка отпущена
        paramsReleased.put("pointerType", "mouse");
        if (currentPageTime != null){
            Double releaseTime = currentPageTime + 95;
            paramsReleased.put("timestamp", releaseTime);
        }

        ObjectNode requestReleased = buildBase("Input.dispatchMouseEvent", paramsReleased);
        String jsonReleased = serializeNode(requestReleased);

        // Объединяем запросы (если нужно отправить их отдельно, можно вернуть их по отдельности)
        return new String[]{jsonPressed, jsonReleased};
    }

    public String[] genMouseClick(int x, int y){
        return genMouseClick(x, y, null);
    }
    
    public String genWindowId(){
        return serializeNode(buildBase("Browser.getWindowForTarget", null));
    }

    public String genLayoutMetrics(){
        return serializeNode(buildBase("Page.getLayoutMetrics", null));
    }

    public String genMouseMove(int x, int y){
        ObjectNode paramsPressed = objectMapper.createObjectNode();
        paramsPressed.put("type", "mouseMoved");
        paramsPressed.put("x", x);
        paramsPressed.put("y", y);
        paramsPressed.put("modifiers", 0);
        paramsPressed.put("button", "none");
        paramsPressed.put("buttons", 0);
        paramsPressed.put("pointerType", "mouse");

        ObjectNode requestPressed = buildBase("Input.dispatchMouseEvent", paramsPressed);
        String jsonMoved = serializeNode(requestPressed);

        return jsonMoved;
    }

    public List<String> genTextInput(String text, String elementId){
        List<String> result = new ArrayList<>();
        String focusJs = String.format("var el = document.getElementById('%s');\nel.focus();", elementId);
        result.add(genExecuteJs(focusJs));

        for (char c: text.toCharArray()){
            // Обработчик для символа (char)
            ObjectNode paramsChar = objectMapper.createObjectNode();
            paramsChar.put("type", "char");
            paramsChar.put("text", String.valueOf(c));
            
            var base = buildBase("Input.dispatchKeyEvent", paramsChar);

            result.add(serializeNode(base));
        }

        return result;
    }

    public String genCaptureScreenshot(){
        ObjectNode params = objectMapper.createObjectNode();
        params.put("format", "png");

        ObjectNode request = buildBase("Page.captureScreenshot", params);

        return serializeNode(request);
    }

    public Optional<String> getScreenshotData(String response){
        return Optional.ofNullable(getJsResult(response, "data"));
    }

    public String[] genKeyInput(){
        ObjectNode paramsChar = objectMapper.createObjectNode();
        paramsChar.put("type", "keyDown");
        paramsChar.put("key", "d");
        paramsChar.put("code", "KeyD");
        paramsChar.put("keyCode", 68);
        paramsChar.put("modifiers", 0); // Предположим, что модификаторов нет
        paramsChar.put("timestamp", 1396.8999999999069);
        paramsChar.put("unmodifiedText", "d");
        paramsChar.put("keyIdentifier", "KeyD");
        paramsChar.put("windowsVirtualKeyCode", 68);
        paramsChar.put("nativeVirtualKeyCode", 68);
        paramsChar.put("autoRepeat", false);
        paramsChar.put("isKeypad", false);
        paramsChar.put("isSystemKey", true);
        paramsChar.put("location", 0);

        ObjectNode paramsChar2 = objectMapper.createObjectNode();
        paramsChar2.put("type", "keyUp");
        paramsChar2.put("key", "d");
        paramsChar2.put("code", "KeyD");
        paramsChar2.put("keyCode", 68);
        paramsChar2.put("modifiers", 0); // Предположим, что модификаторов нет
        paramsChar2.put("timestamp", 1591.8999999999069);
        paramsChar2.put("unmodifiedText", "d");
        paramsChar2.put("keyIdentifier", "KeyD");
        paramsChar2.put("windowsVirtualKeyCode", 68);
        paramsChar2.put("nativeVirtualKeyCode", 68);
        paramsChar2.put("autoRepeat", false);
        paramsChar2.put("isKeypad", false);
        paramsChar2.put("isSystemKey", true);
        paramsChar2.put("location", 0);

        return new String[]{
            serializeNode(buildBase("Input.dispatchKeyEvent", paramsChar)),
            serializeNode(buildBase("Input.dispatchKeyEvent", paramsChar2))
        };
    }

    // иногда нам может в консоль прийти что-то раньше чем результат выполнения джава скрипта
    @Nullable
    public String getJsResult(String json, String fieldName){
        try {
            JsonNode response = objectMapper.readTree(json);
            JsonNode result = response.findValue(fieldName);
            if (result != null){
                return result.asText();
            }
            else{
                throw new Exception("There is no result fild");
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Optional<Integer> parseIdFromCommand(String json){
        try{
            JsonNode command = objectMapper.readTree(json);
            return Optional.of(Integer.parseInt(command.get("id").asText()));
        }
        catch (Exception ex){
            return Optional.empty();
        }
    }

    @Nullable
    public String getJsResult(String json){
        return getJsResult(json, "value");
    }
}
