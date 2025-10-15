package com.dianastore.services;

import com.dianastore.dto.ProductPDPResponse;
import com.dianastore.entities.Price;
import com.dianastore.entities.Product;
import com.dianastore.repository.ProductRepository;
import com.dianastore.repository.PriceRepository;
import org.springframework.stereotype.Service;

@Service
public class PDPService {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    public PDPService(ProductRepository productRepository,PriceRepository priceRepository) {
        this.productRepository = productRepository;
        this.priceRepository=priceRepository;
    }

    public ProductPDPResponse getProductDetails(String country, String sku, String lang) {
        Product product = productRepository.findByCountryCodeAndSku(country, sku);
        Price price=priceRepository.findByCountryCodeAndSku(country,sku);
        if (product == null) {
            throw new RuntimeException("Product with SKU " + sku + " not found in country " + country);
        }

        return new ProductPDPResponse(
                product.getSku(),
                product.getProductName(),
                product.getShortDescription(),
                product.getMediumDescription(),
                product.getLongDescription(),
                price.getMsrp(),
                price.getDiscount(),
                price.getSellingPrice(),
                product.getImageUrl(),
                product.getSize(),
                product.getColour(),
                product.getStyle()
        );
    }
}
