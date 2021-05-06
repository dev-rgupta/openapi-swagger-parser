package com.openapi.pact.gen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.openapi.pact.PactFactory;
import com.openapi.pact.PactJsonGenerator;
import com.openapi.pact.Service;
import com.openapi.pact.model.Pact;

import io.swagger.v3.oas.models.OpenAPI;

public class PactGenerator {

    private  PactFactory pactFactory;
    private  PactJsonGenerator pactJsonGenerator;

    public PactGenerator() {
        this.pactFactory = new PactFactory();
        this.pactJsonGenerator = new PactJsonGenerator();
    }

   
	public PactGenerator(PactFactory pactFactory,
						 PactJsonGenerator pactJsonGenerator) {
		this.pactFactory = pactFactory;
		this.pactJsonGenerator = pactJsonGenerator;
	}


    public void writePactFiles(@NotNull OpenAPI oapi,
                               @NotNull String consumerName) {
        this.write( oapi, consumerName, null);
    }

    public void writePactFiles(@NotNull OpenAPI oapi,
                               @NotNull String consumerName,
                               @NotNull File pactFilesDestinationDir) {
        this.write(oapi, consumerName, pactFilesDestinationDir);
    }

    private void write(OpenAPI oapi, String consumerName,File pactFilesDestinationDir) {
    	//Generates Pacts
        Multimap<Service, Pact> providerToPactMap = generatePacts(oapi, consumerName);
        List<Pact> pacts = providerToPactMap.keySet().stream()
            .map(providerToPactMap::get)
            .map(this::combinePactsToOne)
            .collect(Collectors.toList());

        pactJsonGenerator.writePactFiles(pactFilesDestinationDir, pacts);
    }

	private Multimap<Service, Pact> generatePacts(OpenAPI oapi, String consumerName) {
		Multimap<Service, Pact> providerToPactMap = HashMultimap.create();
		Pact pact = pactFactory.createPacts( oapi,consumerName);
		providerToPactMap.put(pact.getProvider(), pact);
		return providerToPactMap;
	}

    private Pact combinePactsToOne(Collection<Pact> pacts) {
        if (pacts == null || pacts.isEmpty()) {
            return null;
        }
        Pact referencePact = pacts.iterator().next();

        Pact combinedPact = Pact.builder()
                                .metadata(referencePact.getMetadata())
                                .consumer(referencePact.getConsumer())
                                .provider(referencePact.getProvider())
                                .interactions(new ArrayList<>())
                                .build();
        pacts.forEach(pact -> combinedPact.getInteractions().addAll(pact.getInteractions()));

        return combinedPact;
    }

}
