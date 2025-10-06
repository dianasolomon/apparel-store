package com.dianastore.controllers;

import com.dianastore.dto.ProductPLPResponse;
import com.dianastore.dto.ProductPDPResponse;
import com.dianastore.services.PLPService;
import com.dianastore.services.PDPService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dianastore/apparels")
public class PLP_PDP_Controller {
    private final PDPService pdpService;
    private final PLPService plpService;

    public PLP_PDP_Controller(PLPService plpService,PDPService pdpService) {
        this.plpService = plpService;
        this.pdpService=pdpService;
    }
    @GetMapping({"plp/{region}"})
    public List<ProductPLPResponse> getProducts(
            @PathVariable String region,
            @RequestParam String category,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String colour,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return plpService.getProducts(region, category, size, colour, page, limit);
    }

    @GetMapping("pdp/{country}/{sku}")
    public ProductPDPResponse getProduct(
            @PathVariable String country,
            @PathVariable String sku,
            @RequestParam(defaultValue = "en") String lang) {

        return pdpService.getProductDetails(country, sku, lang);
    }





}
