package com.openapi.pact;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapi.pact.gen.PactGenerationException;
import com.openapi.pact.model.Pact;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PactJsonGenerator {

    private ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public void writePactFiles(File destinationDir, Collection<Pact> pacts) {
        pacts.forEach(pact -> writePactFile(destinationDir, pact));
    }

    private void writePactFile(File destinationDir, Pact pact) {
        if (destinationDir != null && !destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        final String pactFileName = pact.getConsumer().getName() + "-" + pact.getProvider().getName() + ".json";
        try {
            objectMapper.writeValue(new File(destinationDir, pactFileName), pact);
        } catch (IOException ex) {
            log.error("Unable to write {} to json file", pact);
            throw new PactGenerationException("Unable to write pact to json file", ex);
        }
    }
}
