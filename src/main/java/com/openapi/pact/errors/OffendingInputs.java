package com.openapi.pact.errors;

import lombok.Data;

@Data
public class OffendingInputs {
	private String name;
	private String value;
	private String source;

}
