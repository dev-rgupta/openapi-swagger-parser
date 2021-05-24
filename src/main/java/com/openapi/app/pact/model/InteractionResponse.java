package com.openapi.app.pact.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InteractionResponse {

    private final String status;
    private final String statusCode;
    private final Map<String, String> headers;
    private final JsonNode body;
    private final JsonNode matchingRules;
}
