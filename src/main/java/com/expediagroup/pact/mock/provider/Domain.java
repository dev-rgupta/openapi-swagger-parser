package com.expediagroup.pact.mock.provider;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Domain {
	
	private String id;
	private Collection<Capability> capability;

}
