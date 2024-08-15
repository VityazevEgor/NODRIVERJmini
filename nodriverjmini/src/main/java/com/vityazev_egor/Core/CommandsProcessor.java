package com.vityazev_egor.Core;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommandsProcessor {
    private Integer id = 1;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ObjectNode buildBase(String method, ObjectNode params){
        ObjectNode request = objectMapper.createObjectNode();
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);
        id++;

        return request;
    }

    @Nullable
    public String genLoadUrl(String url){
        ObjectNode params = objectMapper.createObjectNode();
        params.put("url", url);

        ObjectNode request = buildBase("Page.navigate", params);
        String json = null;
        
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return json;
    }

    public String genExecuteJs(String expression){
        ObjectNode params = objectMapper.createObjectNode();
        params.put("expression", expression);

        ObjectNode request = buildBase("Runtime.evaluate", params);
        String json = null;
        
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return json;
    }

    public String[] genMouseClick(int x, int y) {

        // Создаем событие mousePressed
        ObjectNode paramsPressed = objectMapper.createObjectNode();
        paramsPressed.put("type", "mousePressed");
        paramsPressed.put("x", x);
        paramsPressed.put("y", y);
        paramsPressed.put("button", "left");
        paramsPressed.put("buttons", 1); // Левую кнопку мыши
        paramsPressed.put("clickCount", 1); // Один клик

        ObjectNode requestPressed = buildBase("Input.dispatchMouseEvent", paramsPressed);
        String jsonPressed = null;
        try {
            jsonPressed = objectMapper.writeValueAsString(requestPressed);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Создаем событие mouseReleased
        ObjectNode paramsReleased = objectMapper.createObjectNode();
        paramsReleased.put("type", "mouseReleased");
        paramsReleased.put("x", x);
        paramsReleased.put("y", y);
        paramsReleased.put("button", "left");
        paramsReleased.put("buttons", 0); // Кнопка отпущена

        ObjectNode requestReleased = buildBase("Input.dispatchMouseEvent", paramsReleased);
        String jsonReleased = null;
        try {
            jsonReleased = objectMapper.writeValueAsString(requestReleased);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Объединяем запросы (если нужно отправить их отдельно, можно вернуть их по отдельности)
        return new String[]{jsonPressed, jsonReleased};
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
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public String getJsResult(String json){
        return getJsResult(json, "value");
    }
}
