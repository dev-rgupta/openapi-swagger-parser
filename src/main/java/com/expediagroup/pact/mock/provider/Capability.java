package com.expediagroup.pact.mock.provider;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Capability {

	private String id;
	private boolean isSelected;
	private String  selectionStatus;
	private String configurations;
}
