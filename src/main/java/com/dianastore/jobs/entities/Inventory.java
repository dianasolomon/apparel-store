package com.dianastore.jobs.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"country_code", "sku"})
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String sku;

    private Integer stock;

    @Column(name = "is_disabled")
    private int isDisabled = 0;

    @Column(name = "feed_id")
    private Long feedId;

    @Column(name = "marked_for_delete")
    private Boolean markedForDelete = false;

    @Column(name = "creation_timestamp", updatable = false)
    private Timestamp creationTimestamp;

    @Column(name = "modified_timestamp")
    private Timestamp modifiedTimestamp;

    // === Lifecycle callbacks ===
    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.creationTimestamp = now;
        this.modifiedTimestamp = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedTimestamp = new Timestamp(System.currentTimeMillis());
    }

    // === Getters & Setters ===
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

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getIsDisabled() {
        return isDisabled;
    }

    public void setIsDisabled(Integer isDisabled) {
        this.isDisabled = isDisabled;
    }

    public Long getFeedId() {
        return feedId;
    }

    public void setFeedId(Long feedId) {
        this.feedId = feedId;
    }

    public Boolean getMarkedForDelete() {
        return markedForDelete;
    }

    public void setMarkedForDelete(Boolean markedForDelete) {
        this.markedForDelete = markedForDelete;
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

    // === equals & hashCode ===
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory)) return false;
        Inventory that = (Inventory) o;
        return Objects.equals(countryCode, that.countryCode) &&
                Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, sku);
    }

    // === toString ===
    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", countryCode='" + countryCode + '\'' +
                ", sku='" + sku + '\'' +
                ", stock=" + stock +
                ", isDisabled=" + isDisabled +
                ", feedId=" + feedId +
                ", markedForDelete=" + markedForDelete +
                ", creationTimestamp=" + creationTimestamp +
                ", modifiedTimestamp=" + modifiedTimestamp +
                '}';
    }
}
