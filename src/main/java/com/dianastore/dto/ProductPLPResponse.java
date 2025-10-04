package com.dianastore.dto;

public class ProductPLPResponse {

    private String sku;
    private String productName;
    private String shortDescription;
    private Double msrp;
    private Double discount;
    private Double sellingPrice;
    private String imageUrl;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public ProductPLPResponse(String sku, String productName, String shortDescription, Double msrp, Double discount, Double sellingPrice, String imageUrl, String size, String colour, String style) {
        this.sku = sku;
        this.productName = productName;
        this.shortDescription = shortDescription;
        this.msrp = msrp;
        this.discount = discount;
        this.sellingPrice = sellingPrice;
        this.imageUrl = imageUrl;
        this.size = size;
        this.colour = colour;
        this.style = style;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
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

    private String size;
    private String colour;
    private String style;
}
