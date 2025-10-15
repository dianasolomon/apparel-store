package com.dianastore.dto;


import java.util.List;

public class ProductPDPResponse {

    private String sku;
    private String productName;
    private String shortDescription;
    private String mediumDescription;
    private String longDescription;
    private Double msrp;
    private Double discount;
    private Double sellingPrice;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public ProductPDPResponse(String sku, String productName, String shortDescription, String mediumDescription, String longDescription, Double msrp, Double discount, Double sellingPrice, String imageUrl, String size, String colour, String style) {
        this.sku = sku;
        this.productName = productName;
        this.shortDescription = shortDescription;
        this.mediumDescription = mediumDescription;
        this.longDescription = longDescription;
        this.msrp = msrp;
        this.discount = discount;
        this.sellingPrice = sellingPrice;
        this.imageUrl = imageUrl;
        this.size = size;
        this.colour = colour;
        this.style = style;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getMediumDescription() {
        return mediumDescription;
    }

    public void setMediumDescription(String mediumDescription) {
        this.mediumDescription = mediumDescription;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public Double getMsrp() {
        return msrp;
    }

    public void setMsrp(Double msrp) {
        this.msrp = msrp;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    private String imageUrl;
    private String size;
    private String colour;
    private String style;

    // Getters & Setters
}

