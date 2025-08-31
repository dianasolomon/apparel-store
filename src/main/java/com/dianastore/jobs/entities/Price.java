package com.dianastore.jobs.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(
        name = "prices",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"country_code", "sku"})
        }
)
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;

    @Column(name = "country_code")
    private String countryCode;

    private Double msrp;
    private Double discount;

    @Column(name = "selling_price")
    private Double sellingPrice;

    @Column(name = "creation_ts")
    private Timestamp creationTimestamp;

    @Column(name = "modified_ts")
    private Timestamp modifiedTimestamp;

    @Column(name = "feed_id")
    private Long feedId;

    @Column(name = "marked_for_delete")
    private boolean markedForDelete;

    // --- getters & setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public Double getMsrp() { return msrp; }
    public void setMsrp(Double msrp) { this.msrp = msrp; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }

    public Timestamp getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(Timestamp creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public Timestamp getModifiedTimestamp() { return modifiedTimestamp; }
    public void setModifiedTimestamp(Timestamp modifiedTimestamp) { this.modifiedTimestamp = modifiedTimestamp; }

    public Long getFeedId() { return feedId; }
    public void setFeedId(Long feedId) { this.feedId = feedId; }

    public boolean isMarkedForDelete() { return markedForDelete; }
    public void setMarkedForDelete(boolean markedForDelete) { this.markedForDelete = markedForDelete; }
}
