package com.vityazev_egor.Core;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CDPCommandBuilder {
    private static Integer globalId = 1;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ObjectNode request;
    private final ObjectNode params;
    private final CustomLogger logger = new CustomLogger(CDPCommandBuilder.class.getName());

    private CDPCommandBuilder(String method) {
        this.request = objectMapper.createObjectNode();
        this.params = objectMapper.createObjectNode();
        
        request.put("id", globalId);
        request.put("method", method);
        
        globalId++;
        if (globalId > 2147483) {
            globalId = 1;
        }
    }

    /**
     * Creates a new CDP command builder for the specified method
     */
    public static CDPCommandBuilder create(String method) {
        return new CDPCommandBuilder(method);
    }

    /**
     * Adds a string parameter to the command
     */
    public CDPCommandBuilder addParam(String name, String value) {
        params.put(name, value);
        return this;
    }

    /**
     * Adds an integer parameter to the command
     */
    public CDPCommandBuilder addParam(String name, int value) {
        params.put(name, value);
        return this;
    }

    /**
     * Adds a double parameter to the command
     */
    public CDPCommandBuilder addParam(String name, double value) {
        params.put(name, value);
        return this;
    }

    /**
     * Adds a boolean parameter to the command
     */
    public CDPCommandBuilder addParam(String name, boolean value) {
        params.put(name, value);
        return this;
    }

    /**
     * Builds and returns the JSON string representation of the command
     */
    public String build() {
        if (!params.isEmpty()) {
            request.set("params", params);
        }
        
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.error("Can't build CPD request", e);
            return null;
        }
    }

    // Static utility methods for parsing results

    /**
     * Parses JavaScript result from response
     */
    public static Optional<String> getJsResult(String json, String fieldName) {
        try {
            JsonNode response = objectMapper.readTree(json);
            JsonNode result = response.findValue(fieldName);
            if (result != null) {
                return Optional.ofNullable(result.asText());
            } else {
                throw new Exception("There is no result field");
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Parses JavaScript result from response (default field: "value")
     */
    public static Optional<String> getJsResult(String json) {
        return getJsResult(json, "value");
    }

    /**
     * Parses command ID from JSON
     */
    public static Optional<Integer> parseIdFromCommand(String json) {
        try {
            JsonNode command = objectMapper.readTree(json);
            return Optional.of(Integer.parseInt(command.get("id").asText()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Gets screenshot data from response
     */
    public static Optional<String> getScreenshotData(String response) {
        return getJsResult(response, "data");
    }
}