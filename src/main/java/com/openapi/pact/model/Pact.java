package com.openapi.pact.model;

import java.util.List;

import com.openapi.pact.Services;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pact {

    private final Services provider;
    private final Services consumer;
    private final List<Interaction> interactions;
    private final Metadata metadata;
}
