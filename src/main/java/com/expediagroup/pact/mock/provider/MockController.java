package com.expediagroup.pact.mock.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockController {
    @PostMapping(path ={"/v1/register-domain","/v1/register-capabilities"})
    public ResponseEntity<Object> get() {
       Capability cap = new Capability("id", false, "selectionStatus", "config");
       List<Capability> capList = new ArrayList<>();
       capList.add(cap);
       Domain domain = new Domain("id",capList);
       
       List<Domain> domList = new ArrayList<>();
       domList.add(domain);
       RetrieveProductCatalogResponse res = new RetrieveProductCatalogResponse(domList);
       
       return ResponseEntity.ok(res);
    }
    @GetMapping(path ={"/v1/retrieve-product-catalog"})
    public ResponseEntity<RetrieveProductCatalogResponse> post(@RequestParam String partner_account_id) {
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
