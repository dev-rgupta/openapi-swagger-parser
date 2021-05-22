package com.openapi.app.service;

import java.awt.Color;
import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.openapi.pact.gen.PactGenerator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PactGeneratorRunner  implements ApplicationRunner  {

	private static void generatePactFromOpenAPI(String resourcePath, String consumerName, String producerName,
			String version, PactGenerator pactGenerator) {
		try {
			ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolve(true);
			parseOptions.setResolveFully(true);
			parseOptions.setResolveCombinators(true);
			SwaggerParseResult result = new OpenAPIV3Parser().readLocation(resourcePath, null, parseOptions);
			OpenAPI openAPI = result.getOpenAPI();
			pactGenerator.writePactFiles(openAPI, consumerName, producerName,version, new File("target/pacts/"));
			// logger.info("--- openAPI ---" + openAPI.toString());
		} catch (IllegalArgumentException e) {
			log.error(String.format("%s is not a file or is an invalid URL", resourcePath), Color.RED);
		} catch (NullPointerException e) {
			e.printStackTrace();
			log.error(String.format("The OpenAPI specification in %s is ill formed and cannot be parsed", resourcePath),
					Color.RED);
		}
	}

	/** commandLind Args 
	 * String consumerName,
	 * String producerName,
	 * String pactFilesDestinationDir
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("::::::::::::::consumerName::::::::::::"+consumerName);
		log.info("::::::::::::::producerName::::::::::::"+providerName);
		log.info("::::::::::::::pactSpecificationVersion::::::::::::"+pactSpecificationVersion);
		log.info("::::::::::::::resourcePath::::::::::::"+resourcePath);
		
	//	String resourcePath = "src/main/resources/openapi4.yaml";
		PactGenerator pactGenerator = new PactGenerator();
		generatePactFromOpenAPI(resourcePath, consumerName, providerName,pactSpecificationVersion,pactGenerator);
	}
	
	@Value("${consumer.name}")
	private String consumerName;
	@Value("${provider.name}")
	private  String providerName;
	@Value("${pact.specs.version}")
	private  String pactSpecificationVersion;
	@Value("${resource.path}")
	private  String resourcePath;
	
	
}
