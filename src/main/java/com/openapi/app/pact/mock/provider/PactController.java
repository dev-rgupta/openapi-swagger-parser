package com.openapi.app.pact.mock.provider;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class PactController {
	@RequestMapping(value = "/pactStateChange", method = RequestMethod.POST)
	public Object providerState(@RequestBody String body) {
		log.info("Pact State Change >> set ..."+body);
//		switch (body.getState()) {
//		case "register-domain":
//			log.info("Pact State Change >> set ..."+body.getState());
//			break;
//		case "register-capabilities":
//			log.info("Pact State Change >> set ..."+body.getState());
//			break;
//		case "retrieve-product-catalog":
//			log.info("Pact State Change >> set ..."+body.getState());
//			break;
//		}

		// This response is not mandatory for Pact state change. The only reason is the
		// current Pact-JVM v4.0.3 does
		// check the stateChange request's response, more exactly, checking the
		// response's Content-Type, couldn't be
		// null, so it MUST return something here.
//		PactStateChangeResponseDTO pactStateChangeResponse = new PactStateChangeResponseDTO();
//		pactStateChangeResponse.setState(body.getState());
		return body;
		// return HttpStatus.OK;
	}
}
