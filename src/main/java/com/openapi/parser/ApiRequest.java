package com.openapi.parser;

import lombok.Data;

@Data
public class ApiRequest {
	private String path;
	private String method;
	private String body;
	
}
