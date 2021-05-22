package com.openapi.pact.verification.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openapi.pact.mock.provider.Capability;
import com.openapi.pact.mock.provider.Domain;
import com.openapi.pact.mock.provider.RetrieveProductCatalogResponse;

@RestController
public class InformationController {
    @PostMapping(path ={"/v1/register-domain","/v1/register-capabilities"})
    public ResponseEntity<Object> information() {
       Capability cap = new Capability("id", false, "selectionStatus", "config");
       List<Capability> capList = new ArrayList<>();
       capList.add(cap);
       Domain domain = new Domain("id",capList);
       
       List<Domain> domList = new ArrayList<>();
       domList.add(domain);
       RetrieveProductCatalogResponse res = new RetrieveProductCatalogResponse(domList);
       
       return ResponseEntity.ok(res);
    }
}
