package com.openapi.parser;

import lombok.Data;

@Data
public class ApiResponse {
	private Integer status;
	private ApiResponseHeader headers;
	private String body;
}
