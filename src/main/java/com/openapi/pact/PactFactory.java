package com.openapi.pact;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.openapi.pact.errors.ErrorResources;
import com.openapi.pact.gen.PactGenerationException;
import com.openapi.pact.model.Interaction;
import com.openapi.pact.model.InteractionRequest;
import com.openapi.pact.model.InteractionResponse;
import com.openapi.pact.model.Metadata;
import com.openapi.pact.model.Pact;
import com.openapi.pact.model.PactSpecification;
import com.openapi.pact.model.Param;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@Slf4j
public class PactFactory {

	private static final PodamFactory podamFactory;
	@Autowired
	private static HttpStatus httpStatus;
	Map<String, Object> map = new LinkedHashMap<>();
	static {
		podamFactory = new PodamFactoryImpl();
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(String.class, new EnumStringManufacturer());
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigInteger.class, new BigIntegerManufacturer());
		podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigDecimal.class, new BigDecimalManufacturer());
		podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
	}

	public Pact createPacts(OpenAPI oapi, String consumerName) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		return Pact.builder().provider(new Service("test-provider")) // configure
				.consumer(new Service(consumerName)) // configure
				.interactions(createInteractionsFromMethods(oapi, objectMapper, null))
				.metadata(new Metadata(new PactSpecification("1.0.0"))) // configure
				.build();
	}

	private  List<Interaction> createInteractionsFromMethods(OpenAPI oapi, ObjectMapper objectMapper,
			String pathPrefix) {
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
		InteractionRequest interactionRequest = InteractionRequest.builder().method(entry.getKey())
				.path(requestPathParamGenerator(entry.getValue(), objectMapper, path))
				.headers(requestHeaderParamGenerator(entry.getValue(), objectMapper))
				.query(requestQueryParamGenerator(entry.getValue(), objectMapper))
				.body(requestBodyGenerator(entry.getValue(), objectMapper)).build();
		
		Iterator<Entry<String, ApiResponse>> apiResponseEntryItr = entry.getValue().getResponses().entrySet()
				.iterator();
		
		while (apiResponseEntryItr.hasNext()) {
			Entry<String, ApiResponse> apiResponseEntry = apiResponseEntryItr.next();
			InteractionResponse interactionResponse = InteractionResponse.builder().status(apiResponseEntry.getKey())
					.headers(responseHeaderParamGenerator(apiResponseEntry.getValue().getHeaders()))
					.body(responseBodyGenerator(apiResponseEntry, objectMapper)).build();
			
			Interaction intraction = Interaction.builder()
					.description(entry.getValue().getSummary())
					.request(interactionRequest)
					.response(interactionResponse)
					.build();
			
			interactionList.add(intraction);

		}
		return interactionList;
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
						.defaultValue(manufacturePojo(getClassType(parameter.getSchema().getType()))).build();
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
					.defaultValue(manufacturePojo(getClassType(entry.getValue().getSchema().getType()))).build();
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
				Iterator<Map.Entry<String, Schema>> itr = contentEntryIterator.next().getValue().getSchema()
						.getProperties().entrySet().iterator();
				while (itr.hasNext()) {	
					resultList = extractedParamList(resultList, itr);
					}
			}
			//objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			String jsonString = objectMapper.writeValueAsString(parseParametersToBody(resultList));
		//	log.info("::::::::Request Body String :::"+jsonString);	
			JsonNode  jsonNode = objectMapper.readTree(jsonString);
			log.info("::::::::Request Body JSON :::"+jsonNode);
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
				} else {
					ArraySchema arrayModel = (ArraySchema) schema;
					Schema<?> schema1 = arrayModel.getItems();
					if (schema1.getProperties() != null) {
						Iterator<Map.Entry<String, Schema>> arrayProp = schema1.getProperties().entrySet().iterator();
						while (arrayProp.hasNext()) {
							resultList = extractedParamList(resultList, arrayProp);
						}
					}
				}
				if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {//2xx
					map = parseParametersToBody(resultList);
					jsonString = objectMapper.writeValueAsString(map);
				} else if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).isError()) {//only 4xx and 5xx
					errors = errors.getCompleteDescription(Integer.parseInt(apiResponseEntry.getKey()));
					jsonString = objectMapper.writeValueAsString(errors);

				}

			}
			
		//	String jsonString = objectMapper.writeValueAsString(map);
			
		//	log.info("::::::::Request Body String :::"+jsonString);	
			JsonNode  jsonNode = objectMapper.readTree(jsonString);
			log.info("::::::::Response Body JSON :::"+jsonNode);
			return jsonNode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return objectMapper.nullNode();
	}

	private static List<Param> extractedElementFromArraySchema(Entry<String, Schema> entry) {
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
			param = Param.builder().name(entry.getKey()).type(arrayModel.getType()).defaultValue(schema.getExample())
					.build();
			resultList.add(param);
		}
		return resultList;
	}
	
	private static List<Param> extractedParamList(List<Param> resultList, Iterator<Map.Entry<String, Schema>> entry) {
		Param param=null;
		Entry<String, Schema> entry1 = entry.next();
		if ("array".equalsIgnoreCase(entry1.getValue().getType())) {
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.defaultValue(extractedElementFromArraySchema(entry1)).build();
			resultList.add(param);
		} else if ("object".equalsIgnoreCase(entry1.getValue().getType())) {
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.defaultValue(extractedElementFromObjectSchema(entry1)).build();
			resultList.add(param);
		} else {
			param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
					.defaultValue(entry1.getValue().getExample()).build();
			resultList.add(param);
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
		if (param.getDefaultValue() != null) {
			return param.getDefaultValue();
		}
		return manufacturePojo(getClassType(param.getType()));
	}

	@SuppressWarnings("unchecked")
	private static Object getParamValueFromArray(Param param) {
		// result is [{ "abc":"test","bed":"cot","help":"me"}]
		Map<String, Object> objMap=	parseParametersToBody((List<Param>) param.getDefaultValue());
		
		List<Object> object = new ArrayList<>();
		// simply put obj into ArrayList
		object.add(objMap);
		// result is [{ "abc":"test","bed":"cot","help":"me"}]
	    // log.info("::::::getParamValue:::::jsonArray:::"	+ objMap);
		
		return object;
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
				if (param.getType() == "array" || param.getType() == "object") {
					mapParam.put(param.getName(), getParamValueFromArray(param));
				} else {
					mapParam.put(param.getName(), getParamValue(param));
				}
			//	log.info("::::::parseParametersToBody:::::Key:::" + param.getName() + "::::Value:::::"+ mapParam.get(param.getName()));
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
		}else {
			return String.class;
		}
	}

}
