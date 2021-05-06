package com.openapi.pact.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InteractionRequest {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String query;
    private final JsonNode  body;
    private final JsonNode matchingRules;
}
