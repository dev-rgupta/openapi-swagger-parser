package com.expediagroup.pact.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.expediagroup.pact.model.Pact;
import com.expediagroup.pact.model.Services;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.swagger.v3.oas.models.OpenAPI;

@Component
public class PactGenerator {
	@Autowired
	private PactFactory pactFactory;
	@Autowired
	private PactJsonGenerator pactJsonGenerator;
	
    public void writePactFiles(@NotNull OpenAPI oapi,
                               @NotNull String consumerName,
                               @NotNull String producerName,
                               @NotNull String version) {
        this.write( oapi, consumerName, producerName,version,null);
    }

    public void writePactFiles(@NotNull OpenAPI oapi,
                               @NotNull String consumerName,
                               @NotNull String producerName,
                               @NotNull String version,
                               @NotNull File pactFilesDestinationDir) {
        this.write(oapi, consumerName,producerName, version,pactFilesDestinationDir);
    }

    private void write(OpenAPI oapi, String consumerName,String producerName,String version,File pactFilesDestinationDir) {
    	//Generates Pacts
		Multimap<Services, Pact> providerToPactMap = generatePacts(oapi, consumerName, producerName, version);
        List<Pact> pacts = providerToPactMap.keySet().stream()
            .map(providerToPactMap::get)
            .map(this::combinePactsToOne)
            .collect(Collectors.toList());

        pactJsonGenerator.writePactFiles(pactFilesDestinationDir, pacts);
    }

	private Multimap<Services, Pact> generatePacts(OpenAPI oapi, String consumerName, String producerName,String version) {
		Multimap<Services, Pact> providerToPactMap = HashMultimap.create();
		Pact pact = pactFactory.createPacts( oapi,consumerName,producerName,version);
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
