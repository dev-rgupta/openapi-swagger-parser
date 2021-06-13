package com.expediagroup.pact.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.expediagroup.pact.exception.PactGenerationException;
import com.expediagroup.pact.exception.PropertiesNotFountException;
import com.expediagroup.pact.model.ErrorResources;
import com.expediagroup.pact.model.Interaction;
import com.expediagroup.pact.model.InteractionRequest;
import com.expediagroup.pact.model.InteractionResponse;
import com.expediagroup.pact.model.Metadata;
import com.expediagroup.pact.model.Pact;
import com.expediagroup.pact.model.PactSpecification;
import com.expediagroup.pact.model.Param;
import com.expediagroup.pact.model.Services;
import com.expediagroup.pact.podam.BigDecimalManufacturer;
import com.expediagroup.pact.podam.BigIntegerManufacturer;
import com.expediagroup.pact.podam.EnumStringManufacturer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@Slf4j
@Component
public class PactFactory {
	private static final PodamFactory podamFactory;
	
	@Value("${isOnlyHappyCase}")
	private boolean isOnlyHappyCase=true;
	
	static {
		podamFactory = new PodamFactoryImpl();
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(String.class, new EnumStringManufacturer());
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigInteger.class, new BigIntegerManufacturer());
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigDecimal.class, new BigDecimalManufacturer());
		podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
	}

	public Pact createPacts(OpenAPI oapi, String consumerName, String producerName, String pactSpecificationVersion) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		return Pact.builder()
				.provider(new Services(producerName)) // configure
				.consumer(new Services(consumerName)) // configure
				.interactions(createInteractionsFromMethods(oapi, objectMapper))
				.metadata(new Metadata(new PactSpecification(pactSpecificationVersion))) // configure
				.build();
	}

	private  List<Interaction> createInteractionsFromMethods(OpenAPI oapi, ObjectMapper objectMapper) {
		List<Interaction> interactionResults = new ArrayList<>();
		Paths paths = oapi.getPaths();
		for (Map.Entry<String, PathItem> pathItem : paths.entrySet()) {
			for (Map<String, Operation> opsMap : getListRequestOperations(pathItem.getValue())) {
				Iterator<Entry<String, Operation>> ops = opsMap.entrySet().iterator();
				while (ops.hasNext()) {
					List<Interaction> interactionList = new ArrayList<>();
					Entry<String, Operation> entry = ops.next();
					interactionList =	getInteractionsForEveryResponseCode(entry,pathItem.getKey(),objectMapper);
					interactionResults.addAll(interactionList);
				}
			}
		}
		return interactionResults;
	}

	private  List<Interaction> getInteractionsForEveryResponseCode(Entry<String, Operation> entry, String path,
			ObjectMapper objectMapper) {
		List<Interaction> interactionList = new LinkedList<>();
		InteractionRequest interactionRequest = prepareInteractionRequest(entry, path, objectMapper);

		Iterator<Entry<String, ApiResponse>> apiResponseEntryItr = entry.getValue().getResponses().entrySet()
				.iterator();
		while (apiResponseEntryItr.hasNext()) {
			InteractionResponse interactionResponse = null;
			Interaction intraction = null;
			Entry<String, ApiResponse> apiResponseEntry = apiResponseEntryItr.next();
			if (isOnlyHappyCase) {
				interactionResponse = prepareHappyInteractionResponse(objectMapper, apiResponseEntry);
				if(Objects.nonNull(interactionResponse)) {
				intraction = prepareInteraction(entry, interactionRequest, interactionResponse);
				interactionList.add(intraction);
				}
			} else {
				interactionResponse = prepareInteractionResponse(objectMapper, apiResponseEntry);
				intraction = prepareInteraction(entry, interactionRequest, interactionResponse);
				interactionList.add(intraction);
			}
		}
		return interactionList;
	}
 
	private Interaction prepareInteraction(Entry<String, Operation> entry, InteractionRequest interactionRequest,
			InteractionResponse interactionResponse) {
		return Interaction.builder()
				.description(entry.getValue().getDescription() != null ? entry.getValue().getDescription(): entry.getValue().getSummary())
				.providerState(entry.getValue().getOperationId())
				.request(interactionRequest)
				.response(interactionResponse).build();
	}

	private InteractionRequest prepareInteractionRequest(Entry<String, Operation> entry, String path,
			ObjectMapper objectMapper) {
		return  InteractionRequest.builder().method(entry.getKey())
				.path(requestPathParamGenerator(entry.getValue(), objectMapper, path))
				.headers(requestHeaderParamGenerator(entry.getValue(), objectMapper))
				.query(requestQueryParamGenerator(entry.getValue(), objectMapper))
				.body(requestBodyGenerator(entry.getValue(), objectMapper)).build();
	}

	public static void testWithPactDSLJsonBody(JSONObject body) {
	        Map<String, String> headers = new HashMap<String, String>();
	        headers.put("Content-Type", "application/json;charset=UTF-8");
	     //   PactDslWithProvider builder = new PactDslWithProvider(new ConsumerPactBuilder("consumer"), "provider");
	        
	        DslPart body1 = new PactDslJsonBody()
	                .numberType("salary", 45000)
	                .stringType("name", "Hatsune Miku")
	                .stringType("nationality", "Japan")
	                .object("contact")
	                .stringValue("Email", "hatsune.miku@ariman.com")
	                .stringValue("Phone Number", "9090950")
	                .closeObject();

	        RequestResponsePact pact1 = ConsumerPactBuilder
	                .consumer("JunitDSLJsonBodyConsumerMatching")
	                .hasPactWith("ExampleProvider")
	                .given("")
	                .uponReceiving("Query name is Miku")
	                .path("/information")
	                .query("name=Miku")
	                .method("GET")
	                .willRespondWith()
	                .headers(headers)
	                .status(200)
	                .body(body1)
	                .toPact();
	      
	        System.out.println(":::::::pact1::::::::" + pact1.toString());
			pact1.write("pacts/", PactSpecVersion.V3);

	        RequestResponsePact pact = ConsumerPactBuilder
	                .consumer("JunitDSLJsonBodyConsumer")
	                .hasPactWith("ExampleProvider")
	                .given("")
	                .uponReceiving("Query name is Miku")
	                .path("/information")
	                .query("name=Miku")
	                .method("GET")
	                .willRespondWith()
	                .headers(headers)
	                .status(200)
	                .body(body)
	                .toPact();
	        
	        System.out.println(":::::::pact::::::::" + pact.toString());
			pact.write("pacts/", PactSpecVersion.V3);
	    }
	 
	private InteractionResponse prepareHappyInteractionResponse(ObjectMapper objectMapper, Entry<String, ApiResponse> apiResponseEntry) {
		InteractionResponse interactionResponse=null;
		//HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()
		if (!"default".equalsIgnoreCase(apiResponseEntry.getKey()) && HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {
			interactionResponse = InteractionResponse.builder().status(apiResponseEntry.getKey())
				.headers(responseHeaderParamGenerator(apiResponseEntry.getValue().getHeaders()))
				.body(responseBodyGenerator(apiResponseEntry, objectMapper)).build();
		}
		return interactionResponse;
	}
	
	private InteractionResponse prepareInteractionResponse(ObjectMapper objectMapper, Entry<String, ApiResponse> apiResponseEntry) {
			return  InteractionResponse.builder().status(apiResponseEntry.getKey())
				.headers(responseHeaderParamGenerator(apiResponseEntry.getValue().getHeaders()))
				.body(responseBodyGenerator(apiResponseEntry, objectMapper)).build();
	}

	private static String requestQueryParamGenerator(Operation operation, ObjectMapper objectMapper) {
		List<Parameter> parameterList = operation.getParameters();
		if (parameterList == null) {
			return null;
		}
		StringBuilder queryBuilder = new StringBuilder();
		for (Parameter parameter : parameterList) {
			if ("query".equalsIgnoreCase(parameter.getIn())) {
				queryBuilder.append(parameter.getName()).append("=").append(manufacturePojo(getClassType(parameter.getSchema().getType()))).append("&");
			}
		}
		if (queryBuilder.length() != 0) {
			queryBuilder.deleteCharAt(queryBuilder.length() - 1);
		}

		return queryBuilder.toString();
	}
	
	private static Map<String, String> requestHeaderParamGenerator(Operation operation, ObjectMapper objectMapper) {
		List<Parameter> parameterList = operation.getParameters();
		if (parameterList == null) {
			return null;
		}
		List<Param> headerList = new LinkedList<>();
		Param param=null;
		for (Parameter parameter : parameterList) {
			if ("header".equalsIgnoreCase(parameter.getIn())) {
				param = Param.builder().name(parameter.getName()).type(parameter.getSchema().getType())
						.childNode(manufacturePojo(getClassType(parameter.getSchema().getType())))
						.ref(parameter.getSchema().get$ref()).build();
				headerList.add(param);
			}
		}
		
		return mapHeaders(headerList);
	}
	
	private static Map<String, String> responseHeaderParamGenerator(Map<String, Header> headerMap) {
		if(Objects.nonNull(headerMap)) {
		Iterator<Entry<String, Header>> headerEntry = headerMap.entrySet().iterator();
		List<Param> headerList = new LinkedList<>();
		Param param =null;
		while(headerEntry.hasNext()) {
			Entry<String, Header>  entry = headerEntry.next();
			param = Param.builder().name(entry.getKey()).type(entry.getValue().getSchema().getType())
					.childNode(manufacturePojo(getClassType(entry.getValue().getSchema().getType())))
					.ref(entry.getValue().getSchema().get$ref()).build();
			headerList.add(param);
		}
		return mapHeaders(headerList);
		}
		return null;
	}
	
	private static String requestPathParamGenerator(Operation operation, ObjectMapper objectMapper, String path) {
		List<Parameter> parameterList = operation.getParameters();
		if (parameterList != null) {
			for (Parameter parameter : parameterList) {
				if ("path".equalsIgnoreCase(parameter.getIn())) {
					path = path.replace("{" + parameter.getName() + "}", String.valueOf(manufacturePojo(getClassType(parameter.getSchema().getType()))));
				}
			}
		}
		return path;
	}

	@SuppressWarnings("unchecked")
	private static JsonNode requestBodyGenerator(Operation operation, ObjectMapper objectMapper) {
		List<Param> resultList = new LinkedList<>();
		RequestBody requestBody = null;
		try {
			requestBody = operation.getRequestBody();
			if (Objects.isNull(requestBody)) {
				return null;
			}
			Iterator<Entry<String, MediaType>> contentEntryIterator = requestBody.getContent().entrySet().iterator();
			while (contentEntryIterator.hasNext()) {
				Entry<String, MediaType> mapMediaType = contentEntryIterator.next();
				Schema schema = mapMediaType.getValue().getSchema();
				if (Objects.isNull(schema.getProperties())) {
					// TODO
					log.error(":::: No properties Found in schema {}  ::: " + schema.getName());
				} else {
					Iterator<Map.Entry<String, Schema>> itr = schema.getProperties().entrySet().iterator();
					while (itr.hasNext()) {
						resultList = extractedParamList(resultList, itr);
					}
				}
			}
			String jsonString = objectMapper.writeValueAsString(parseParametersToBody(resultList));
		//	log.info("::::::::Request Body String :::"+jsonString);	
			JsonNode  jsonNode = objectMapper.readTree(jsonString);
		//	log.info("::::::::Request Body JSON :::"+jsonNode);
			return jsonNode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objectMapper.nullNode();
	}
	
	@SuppressWarnings("unchecked")
	private  JsonNode responseBodyGenerator(Entry<String, ApiResponse> apiResponseEntry, ObjectMapper objectMapper) {
		List<Param> resultList = new ArrayList<>();
		if (apiResponseEntry.getValue().getContent() == null) {
			return null;
		}
		Iterator<Entry<String, MediaType>> contentEntryIterator = apiResponseEntry.getValue().getContent().entrySet().iterator();
		try {
			Map<String, Object> map =null;
			String jsonString=null;
			ErrorResources errors = new ErrorResources();
			while (contentEntryIterator.hasNext()) {
				Entry<String, MediaType> entryMediaType = contentEntryIterator.next();
				Schema schema = entryMediaType.getValue().getSchema();
				if (Objects.nonNull(schema.getProperties())) {
					Iterator<Map.Entry<String, Schema>> itr = schema.getProperties().entrySet().iterator();
					while (itr.hasNext()) {
						resultList = extractedParamList(resultList, itr);
					}
				}else if(schema instanceof ArraySchema) {
					ArraySchema arrayModel = (ArraySchema) schema;
					Schema<?> schema1 = arrayModel.getItems();
					if (schema1.getProperties() != null) {
						Iterator<Map.Entry<String, Schema>> arrayProp = schema1.getProperties().entrySet().iterator();
						while (arrayProp.hasNext()) {
							resultList = extractedParamList(resultList, arrayProp);
						}
					}else {
						throw new PropertiesNotFountException();
					}
				}else if(schema instanceof MapSchema) {
					//TODO
					log.error(":::: No Rules for MapSchema  ::: ");
					Schema additionalProperty = (Schema) schema.getAdditionalProperties();
				
				}
				if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {//2xx
					map = parseParametersToBody(resultList);
					jsonString = objectMapper.writeValueAsString(map);
					//testWithPactDSLJsonBody(new JSONObject(jsonString));
				} else if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).isError()) {//only 4xx and 5xx
					errors = errors.getCompleteDescription(Integer.parseInt(apiResponseEntry.getKey()));
					jsonString = objectMapper.writeValueAsString(errors);

				}

			}
			
		//	log.info("::::::::Request Body String :::"+jsonString);	
			JsonNode  jsonNode = objectMapper.readTree(jsonString);
		//	log.info("::::::::Response Body JSON :::"+jsonNode);
			return jsonNode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objectMapper.nullNode();
	}

	private static List<Param> extractedElementFromArraySchema(Entry<String, Schema> entry) throws PropertiesNotFountException {
		Param param = null;
		List<Param> resultList = new ArrayList<>();
		ArraySchema arrayModel = (ArraySchema) entry.getValue();
		Schema<?> schema = arrayModel.getItems();
		if (schema.getProperties() != null) {
			Iterator<Map.Entry<String, Schema>> itr = schema.getProperties().entrySet().iterator();
			while (itr.hasNext()) {
				resultList = extractedParamList(resultList, itr);
			}
		} else {
			throw new PropertiesNotFountException();
		}
		return resultList;
	}
	


	private static List<Param> extractedElementFromComposedSchema(Entry<String, Schema> entry1) {
		List<Param> resultList = new LinkedList<>();
		ComposedSchema composedModel = (ComposedSchema) entry1.getValue();
		List<Schema> schema = composedModel.getAllOf();
		Schema<?> schema1 = schema.get(0);
			Iterator<Map.Entry<String, Schema>> itr = schema1.getProperties().entrySet().iterator();
			while (itr.hasNext()) {
				resultList = extractedParamList(resultList, itr);
			}
		return resultList;
	}

	@SuppressWarnings("unchecked")
	private static List<Param> extractedElementFromObjectSchema(Entry<String, Schema> entry) {
		List<Param> resultList = new LinkedList<>();
		Iterator<Map.Entry<String, Schema>> itr = entry.getValue().getProperties().entrySet().iterator();
		while (itr.hasNext()) {
			resultList = extractedParamList(resultList, itr);
		}
		return resultList;
	}
	
	private static List<Param> extractedParamList(List<Param> resultList, Iterator<Map.Entry<String, Schema>> entry) {
		Param param = null;
		Entry<String, Schema> entry1 = entry.next();
		if (entry1.getValue() instanceof ArraySchema) {
			// log.info("::::::::::::::::in ArraySchema::::::::::::::");
			try {
				param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
						.childNode(extractedElementFromArraySchema(entry1)).ref(entry1.getValue().get$ref()).build();
			} catch (PropertiesNotFountException pnfe) {
				 log.info("::::::No properties in ArraySchema:::::::"+entry1.getKey());
				param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
						.childNode(entry1.getValue().getExample()).ref(entry1.getValue().get$ref()).build();
			}
			resultList.add(param);
		} else if (entry1.getValue() instanceof ObjectSchema) {
			// log.info("::::::::::::::::in ObjectSchema::::::::::::::");
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.childNode(extractedElementFromObjectSchema(entry1)).ref(entry1.getValue().get$ref()).build();
			resultList.add(param);
		} else if (entry1.getValue() instanceof ComposedSchema) {
			// log.info("::::::::::::::::in ComposedSchema::::::::::::::");
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.childNode(extractedElementFromComposedSchema(entry1)).ref(entry1.getValue().get$ref()).build();
			resultList.add(param);

		} else {
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.childNode(entry1.getValue().getExample()).ref(entry1.getValue().get$ref()).build();
			resultList.add(param);
		}
		return resultList;
	}

	private static List<Map<String, Operation>> getListRequestOperations(PathItem p) {
		List<Map<String, Operation>> opsList = new LinkedList<>();
		if (p.getGet() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("GET", p.getGet());
			opsList.add(opsMap);
		}
		if (p.getPut() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("PUT", p.getPut());
			opsList.add(opsMap);
		}
		if (p.getPost() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("POST", p.getPost());
			opsList.add(opsMap);
		}
		if (p.getDelete() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("DELETE", p.getDelete());
			opsList.add(opsMap);
		}
		if (p.getOptions() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("OPTIONS", p.getOptions());
			opsList.add(opsMap);
		}
		if (p.getHead() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("HEAD", p.getHead());
			opsList.add(opsMap);
		}
		if (p.getPatch() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("PATCH", p.getPatch());
			opsList.add(opsMap);
		}
		if (p.getTrace() != null) {
			Map<String, Operation> opsMap = new HashMap<>();
			opsMap.put("TRACE", p.getTrace());
			opsList.add(opsMap);
		}
		return opsList;
	}

	private static Object getParamValue(Param param) {
		if (param.getChildNode() != null) {
			return param.getChildNode();
		}
		return manufacturePojo(getClassType(param.getType()));
	}

	@SuppressWarnings("unchecked")
	private static Object getParamValueFromArray(Param param) {
		if(param.getChildNode() instanceof String) {
			return param.getChildNode();
		}
		if(param.getChildNode() instanceof ArrayNode) {
			return param.getChildNode();
		}
		
		Map<String, Object> objMap = parseParametersToBody((List<Param>) param.getChildNode());
		List<Object> object = new ArrayList<>();
		object.add(objMap);
		// log.info("::::::getParamValue:::::jsonArray:::" + objMap);
		return object;
	}
	@SuppressWarnings("unchecked")
	private static Object getParamValueFromObject(Param param) {
		Map<String, Object> objMap = parseParametersToBody((List<Param>) param.getChildNode());
		return objMap;
	}
	
	private static Object manufacturePojo(Class<?> type) {
		Object manufacturedPojo = podamFactory.manufacturePojo(type);
		if (manufacturedPojo == null) {
			throw new PactGenerationException("Podam manufacturing failed");
		}
		return manufacturedPojo;
	}

	private static Map<String, Object> parseParametersToBody(List<Param> requestParameters) {
		Map<String, Object> mapParam = new LinkedHashMap<>();
		if(Objects.nonNull(requestParameters)) {
			requestParameters.forEach(param -> {
				if (param.getType() == null && param.getRef() != null) {
					// log.warn("::::::::::::::param.getRef():::::::::::::::" + param.getRef());
					mapParam.put(param.getName(), getParamValue(param));
				} else if (param.getType() == "object" || param.getType() == null) {
					// log.warn("::::::::::::::object Body:::::::::::::::" + param.getType());
					mapParam.put(param.getName(), getParamValueFromObject(param));
				} else if (param.getType() == "array") {
					// log.warn("::::::::::::::array and null Body:::::::::::::::" +
					// param.getType());
					if (param.getChildNode() != null) {
						mapParam.put(param.getName(), getParamValueFromArray(param));
					} else {
						mapParam.put(param.getName(), getParamValue(param));
					}
				} else {
					// log.warn("::::::::::::::default:::::::::::::::" + param.getType());
					mapParam.put(param.getName(), getParamValue(param));
				}
				// log.info("::::::parseParametersToBody:::::Key:::" + param.getName()
				// +"::::Value:::::"+ mapParam.get(param.getName()));
			});
		}
		return mapParam;
	}


	private static Map<String, String> mapHeaders(List<Param> headers) {
		Map<String, List<String>> mappedHeadersWithDuplicates = mapHeadersWithDuplicates(headers);

		Map<String, String> resultingHeaders = new HashMap<>();
		for (String key : mappedHeadersWithDuplicates.keySet()) {
			if (mappedHeadersWithDuplicates.get(key).size() > 1) {
				log.warn("More than one value for header: {}", key);
			}
			resultingHeaders.put(key, mappedHeadersWithDuplicates.get(key).get(0));
		}
		return resultingHeaders;
	}

	private static Map<String, List<String>> mapHeadersWithDuplicates(List<Param> headers) {
		return headers.stream().collect(Collectors.groupingBy(Param::getName,
				Collectors.mapping(param -> String.valueOf(getParamValue(param)), Collectors.toList())));
	}
	

	private static Class<?> getClassType(String str) {
		if ("boolean".equalsIgnoreCase(str)) {
			return Boolean.class;
		}
		if ("string".equalsIgnoreCase(str)) {
			return String.class;
		}
		if ("integer".equalsIgnoreCase(str)) {
			return Integer.class;
		} else {
			return String.class;
		}
	}


}
