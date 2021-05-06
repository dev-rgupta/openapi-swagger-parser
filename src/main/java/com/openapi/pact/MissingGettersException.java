package com.openapi.pact;

public class MissingGettersException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -7741514245220049334L;
	private static final String MESSAGE_PLACEHOLDER = "Serialization is not possible, %s class has no getters";

    MissingGettersException(String className) {
        super(String.format(MESSAGE_PLACEHOLDER, className));
    }
}
