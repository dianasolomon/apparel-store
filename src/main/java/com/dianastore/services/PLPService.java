package com.dianastore.services;

import com.dianastore.dto.ProductPLPResponse;
import com.dianastore.entities.Product;
import com.dianastore.entities.Price;
import com.dianastore.entities.Inventory;
import com.dianastore.repository.ProductRepository;
import com.dianastore.repository.PriceRepository;
import com.dianastore.repository.InventoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PLPService {

    private static final Logger log = LoggerFactory.getLogger(PLPService.class);

    private final ProductRepository productRepo;
    private final PriceRepository priceRepo;
    private final InventoryRepository inventoryRepo;

    public PLPService(ProductRepository productRepo, PriceRepository priceRepo, InventoryRepository inventoryRepo) {
        this.productRepo = productRepo;
        this.priceRepo = priceRepo;
        this.inventoryRepo = inventoryRepo;
    }
    public List<ProductPLPResponse> getProducts(String region, String category, String size, String colour, int page) {
        String normalizedRegion = region.toUpperCase();

        log.info("Fetching products for region={}, category={}, size={}, colour={}, page={}",
                normalizedRegion, category, size, colour, page);

        Page<Product> products = productRepo.findByCountryCodeAndCategoryContainingIgnoreCase(
                normalizedRegion, category, PageRequest.of(page, 20)
        );

        // Split colour into list if multiple values provided
        // Normalize colour filters to lowercase
        List<String> colourFilters = (colour != null && !colour.isEmpty())
                ? List.of(colour.split(","))
                .stream()
                .map(String::toLowerCase)  // ✅ convert filters to lowercase
                .collect(Collectors.toList())
                : null;

        List<ProductPLPResponse> result = products.stream()
                .filter(p -> (size == null || p.getSize().equalsIgnoreCase(size))) // ✅ size filter
                .filter(p -> (colourFilters == null || colourFilters.contains(p.getColour().toLowerCase()))) // ✅ colour filter
                .map(p -> {
                    Inventory inv = inventoryRepo.findByCountryCodeAndSku(normalizedRegion, p.getSku());
                    if (inv == null || inv.getStock() <= 0) {
                        return null;
                    }

                    Price price = priceRepo.findByCountryCodeAndSku(normalizedRegion, p.getSku());

                    return new ProductPLPResponse(
                            p.getSku(),
                            p.getProductName(),
                            p.getShortDescription(),
                            price != null ? price.getMsrp() : null,
                            price != null ? price.getDiscount() : null,
                            price != null ? price.getSellingPrice() : null,
                            p.getImageUrl(),
                            p.getSize(),
                            p.getColour(),
                            p.getStyle()
                    );
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());

        log.info("✅ Final products returned to client: {}", result.size());
        return result;
    }
}
