package com.openapi.pact.model;

import java.util.List;

import com.openapi.pact.Service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pact {

    private final Service provider;
    private final Service consumer;
    private final List<Interaction> interactions;
    private final Metadata metadata;
}
