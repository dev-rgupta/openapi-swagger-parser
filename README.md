# Openapi Swagger Pact Generator
> This is a Parser That parses OpenAPI definitions in JSON or YAML format and Generates Pact files That contain HTTP request/response interactions by parsing Given Open API specifications.

## NOTE :- Its a Prototype Application and in that we covered Only Happy case and we are working to add more functionalities and add more info in Pact file like MatchingRules, Interactions with other response codes and for now i have used some hard coded thing but later on it will be configurable. like generated file name,directory creation path etc. 

## Requirements
- Java 1.8 or higher
- Maven

## Step to Config and Run the Application 
- Its a Maven based project or Application so you just need to clone this repo and execute few maven commands with required arguments list, that will generates Pact file in publish to the provide pact broker.

before running below command kindly configure pact broker configurations in application.properties file

######## Pact Broker Credentials ########
pactbroker.host=https://dev-rgupta.pactflow.io/
pactbroker.auth.username=gupta.ratnesh9@gmail.com
pactbroker.auth.password=Compaq@2190
pact.verifier.publishResults=true

# by the help of both commands we can generates pacts

 mvn clean install "-Dconsumer.name=consumer"  "-Dprovider.name=provider" "-Dpact.specs.version=1.0.0" "-Dresource.path=src/main/resources/openapi4.yaml"

    OR
    
 java -jar target/openapi-swagger-parser-0.0.1-SNAPSHOT.jar --provider.name=provider --consumer.name=consumer --pact.specs.version=1.0.0 --resource.path=src/main/resources/openapi4.yaml

# to publish pact file into provided pactBroker

 mvn pact:publish
 
----------------------------------------------------------------------------------------------------------------------------------------------------------------
(Kindly validate's file format https://editor.swagger.io/ ) it will Automatically generate Pact file in json format in "target/pacts/"

I have used below sample files for demo and devlopment
- https://github.expedia.biz/eg-business-domains/rest-api-definitions/blob/master/definitions/Partner/Management/PartnerAPI/v1beta/openapi.yml
- https://github.expedia.biz/eg-business-domains/rest-api-definitions/edit/master/definitions/Partner/CapabilitySelection/CapabilitySelectionAPI/v1beta/openapi.yml

## Provider side (java service) changes to verify the pact integration's against implemented API's

In provider service just need to add maven plugin in pom.xml file
Step 1:
```xml
<plugin>
				<groupId>au.com.dius.pact.provider</groupId>
				<artifactId>maven</artifactId>
				<version>4.1.11</version>
				<configuration>
					<pactBrokerUrl>https://dev-rgupta.pactflow.io/</pactBrokerUrl>
					<pactBrokerToken>QoXeUwD8k5OENElS5EuDiA</pactBrokerToken> <!-- Replace TOKEN with the actual token -->
					<pactBrokerAuthenticationScheme>Bearer</pactBrokerAuthenticationScheme>
					<serviceProviders>
						<serviceProvider>
							<stateChangeUrl>http://localhost:8080/pactStateChange</stateChangeUrl>
							<name>provider</name>
						</serviceProvider>
					</serviceProviders>
					<configuration>
						<pact.showStacktrace>true</pact.showStacktrace>
						<pact.verifier.publishResults>true</pact.verifier.publishResults>
					</configuration>
				</configuration>
			</plugin>
```

this plugin have some configuration named below
a: pact broker            ## https://dev-rgupta.pactflow.io/ to pull pact file for verification 
b: state change Url.      ## http://localhost:8080/pactStateChange
c: provider name.         ## provider
d: to publish the result  ## true


Step 2:

Provide All API's implementation

Step 3:

provide an extra controller for pactStateChange that has all the providerSatate's (i',m working on that how we can make easy or avoid this step)

Step 4:

execute below maven command to verify the pact against Provider

mvn pact:verify

it will verify the pact and update the results back to broker and mark the status
