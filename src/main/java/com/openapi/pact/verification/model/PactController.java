package com.openapi.pact.verification.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PactController {

	@RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
	public PactStateChangeResponseDTO providerState(@RequestBody PactState body) {

		switch (body.getState()) {
		case "register-domain":
			Nationality.setNationality(null);
			System.out.println("Pact State Change >> set register-domain ...");
			break;
		case "register-capabilities":
			Nationality.setNationality("Japan");
			System.out.println("Pact Sate Change >> set register-capabilitiesy ...");
			break;
		case "retrieve-product-catalog":
			Nationality.setNationality("Japan");
			System.out.println("Pact Sate Change >> set retrieve-product-catalog ...");
			break;
		}

		// This response is not mandatory for Pact state change. The only reason is the current Pact-JVM v4.0.3 does
		// check the stateChange request's response, more exactly, checking the response's Content-Type, couldn't be
		// null, so it MUST return something here.
		PactStateChangeResponseDTO pactStateChangeResponse = new PactStateChangeResponseDTO();
		pactStateChangeResponse.setState(body.getState());
		return pactStateChangeResponse;
		// return HttpStatus.OK;
	}
}
