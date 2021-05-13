package com.openapi.app;

import java.awt.Color;
import java.io.File;

import org.springframework.stereotype.Component;

import com.openapi.pact.gen.PactGenerator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeneratePactFromOAS /* implements CommandLineRunner */ {
	public static void main(String[] args) {
		String resource = "src/main/resources/openapi4.yaml";
		PactGenerator pactGenerator = new PactGenerator();
		generatePactFromOAS(resource, pactGenerator);
	}

	private static void generatePactFromOAS(String resource, PactGenerator pactGenerator) {
		try {
			ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolve(true);
			parseOptions.setResolveFully(true);
			parseOptions.setResolveCombinators(true);
			SwaggerParseResult result = new OpenAPIV3Parser().readLocation(resource, null, parseOptions);
			OpenAPI openAPI = result.getOpenAPI();
			pactGenerator.writePactFiles(openAPI, "test-consumer", new File("pacts/"));
			// logger.info("--- openAPI ---" + openAPI.toString());
		} catch (IllegalArgumentException e) {
			log.error(String.format("%s is not a file or is an invalid URL", resource), Color.RED);
		} catch (NullPointerException e) {
			e.printStackTrace();
			log.error(String.format("The OpenAPI specification in %s is ill formed and cannot be parsed", resource),
					Color.RED);
		}
	}

//	@Override
//	public void run(String... args) throws Exception {
//		String resource = "src/main/resources/openapi4.yaml";
//		PactGenerator pactGenerator = new PactGenerator();
//		generatePactFromOAS(resource, pactGenerator);	
//	}
}
