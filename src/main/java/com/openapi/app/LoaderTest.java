package com.openapi.app;


import java.awt.Color;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoaderTest  {
  public static void main(String[] args) {
	    String resource = "src/main/resources/openapi4.yaml";

	    try {
	    	ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolve(true);
			parseOptions.setResolveFully(true);
			parseOptions.setResolveCombinators(true);
	    	
	    	// or from a file
	      SwaggerParseResult result = new OpenAPIParser().readLocation(resource, null, parseOptions);
	      OpenAPI openAPI = result.getOpenAPI();
	    //  log.info("--- openAPI ---"+openAPI.toString());
	      log.info("--- paths ---"+openAPI.getPaths().toString());
	    } catch (IllegalArgumentException e) {
	      log.error(String.format("%s is not a file or is an invalid URL", resource),
	          Color.RED);
	    } catch (NullPointerException e) {
	      log.error(String
	              .format("The OpenAPI specification in %s is ill formed and cannot be parsed",
	                  resource),
	          Color.RED);
	    }
	  }
}
