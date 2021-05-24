package com.openapi.app.pact.mock.provider;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetrieveProductCatalogResponse {
	private Collection<Domain> domains;

}
