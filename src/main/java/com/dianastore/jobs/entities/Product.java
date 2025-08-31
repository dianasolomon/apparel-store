package com.dianastore.jobs.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"country_code", "sku"})
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "medium_description", length = 500)
    private String mediumDescription;

    @Lob
    @Column(name = "long_description")
    private String longDescription;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "product_name", nullable = false)
    private String productName;

    private String category;
    private String size;
    private String colour;
    private String style;
    private String notes;

    // <-- use java.sql.Timestamp (single timestamp type for the app)
    @Column(name = "creation_timestamp")
    private Timestamp creationTimestamp;

    @Column(name = "modified_timestamp")
    private Timestamp modifiedTimestamp;

    @Column(name = "out_of_stock")
    private boolean outOfStock;

    // feed tracking (used by importers)
    @Column(name = "feed_id")
    private Long feedId;


    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Timestamp creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Timestamp getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public boolean isOutOfStock() {
        return outOfStock;
    }

    public void setOutOfStock(boolean outOfStock) {
        this.outOfStock = outOfStock;
    }

    public Long getFeedId() {
        return feedId;
    }

    public void setFeedId(Long feedId) {
        this.feedId = feedId;
    }

    // equals/hashCode based on unique business key (countryCode + sku)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product p = (Product) o;
        return Objects.equals(countryCode, p.countryCode) &&
                Objects.equals(sku, p.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, sku);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", countryCode='" + countryCode + '\'' +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                '}';
    }
}
