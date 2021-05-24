package com.openapi.app.pact.service;

import java.awt.Color;
import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import au.com.dius.pact.core.model.PactSpecVersion;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Data
public class PactGeneratorService  implements CommandLineRunner   {
	
	@Autowired
	private PactGenerator pactGenerator;

	private void generatePactFromOpenAPI(String resourcePath, String consumerName, String providerName, String pactSpecificationVersion) {
		try {
			ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolve(true);
			parseOptions.setResolveFully(true);
			parseOptions.setResolveCombinators(true);
			SwaggerParseResult result = new OpenAPIV3Parser().readLocation(resourcePath, null, parseOptions);
			OpenAPI openAPI = result.getOpenAPI();
			pactGenerator.writePactFiles(openAPI, consumerName, providerName,pactSpecificationVersion, new File("target/pacts/"));
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
	public void run(String... args) throws Exception {
		try {
			validateArgs(this);
			generatePactFromOpenAPI(resourcePath, consumerName, providerName, pactSpecificationVersion);
			log.info("######## pact file generated in 'target/pacts' directory and uploaded to pact Broker ########");
		} catch (Exception e) {
			log.info(":::: Something went wrong Error Message is :::: " + e.getMessage());
			//log.error(":::: Something Went wrong Error Stack Trace ::::" + e);
		}
	}
	
	private void validateArgs(PactGeneratorService pactGeneratorRunner) throws Exception {
		if (StringUtils.isEmpty(pactGeneratorRunner.getResourcePath())) {
			log.info("Hint use '-Dresource.path={file path}' to pass resource path");
			throw new Exception("Argument 'resourcePath' can't br null!");
		}
		if (StringUtils.isEmpty(pactGeneratorRunner.getConsumerName())) {
			this.consumerName = this.consumerName + new Date();
		}
		if (StringUtils.isEmpty(pactGeneratorRunner.getProviderName())) {
			this.providerName = this.providerName + new Date();
		}
		if (StringUtils.isEmpty(pactGeneratorRunner.getPactSpecificationVersion())) {
			this.pactSpecificationVersion = PactSpecVersion.V3.toString();
		}
		log.info("::::::::::::::Arguments List ::::::::::::" + this.toString());
	}

	@Value("${consumer.name}")
	private String consumerName;
	@Value("${provider.name}")
	private  String providerName;
	@Value("${pact.specs.version}")
	private  String pactSpecificationVersion;
	@Value("${resource.path}")
	private  String resourcePath;
	
	@Override
	public String toString() {
		return "PactGeneratorRunner [consumerName=" + consumerName + ", providerName=" + providerName
				+ ", pactSpecificationVersion=" + pactSpecificationVersion + ", resourcePath=" + resourcePath + "]";
	}
}
