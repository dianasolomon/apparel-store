package com.dianastore.controllers;

import com.dianastore.dto.ProductPLPResponse;
import com.dianastore.services.PLPService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dianastore/apparels/plp")
public class PLPController {

    private final PLPService plpService;

    public PLPController(PLPService plpService) {
        this.plpService = plpService;
    }

    @GetMapping("/{region}")
    public List<ProductPLPResponse> getProducts(
            @PathVariable String region,
            @RequestParam String category,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String colour,
            @RequestParam(defaultValue = "0") int page
    ) {
        return plpService.getProducts(region, category, size, colour, page);
    }

}
