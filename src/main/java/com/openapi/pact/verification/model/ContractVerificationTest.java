package com.openapi.pact.verification.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

//@SpringBootTest(classes = OpenapiSwaggerParserApplication.class , webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Provider("test-provider")
@PactFolder("pacts/")
class ContractVerificationTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }
    
    @BeforeEach
    void before(PactVerificationContext context) {
   //   context.setTarget(HttpTestTarget.fromUrl(new URL(myProviderUrl)));
      // or something like
       context.setTarget(new HttpTestTarget("localhost", 8080, "/"));
    }
    
}