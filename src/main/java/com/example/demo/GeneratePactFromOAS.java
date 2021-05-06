package com.example.demo;

import java.awt.Color;
import java.io.File;

import com.openapi.pact.gen.PactGenerator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneratePactFromOAS {
	public static void main(String[] args) {
		String resource = "src/main/resources/openapi2.yaml";
		PactGenerator pactGenerator = new PactGenerator();
		generatePactFromOAS(resource, pactGenerator);
	}

	private static void generatePactFromOAS(String resource, PactGenerator pactGenerator) {
		try {
			ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolveFully(true);
			parseOptions.setResolveCombinators(false);
			SwaggerParseResult result = new OpenAPIV3Parser().readLocation(resource, null, parseOptions);
			OpenAPI openAPI = result.getOpenAPI();
			pactGenerator.writePactFiles(openAPI, "test-consumer", new File("pacts/"));
	    //	logger.info("--- openAPI ---" + openAPI.toString());
		} catch (IllegalArgumentException e) {
			log.error(String.format("%s is not a file or is an invalid URL", resource), Color.RED);
		} catch (NullPointerException e) {
			e.printStackTrace();
			log.error(String.format("The OpenAPI specification in %s is ill formed and cannot be parsed", resource),
					Color.RED);
		}
	}
}
