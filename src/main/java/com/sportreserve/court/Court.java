package com.sportreserve.court;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "courts")
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourtType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "court_amenities", joinColumns = @JoinColumn(name = "court_id"))
    @Column(name = "amenity")
    private List<String> amenities;

    public Court() {}

    public Court(String name, CourtType type, String description, Integer durationMinutes,
                 BigDecimal price, String imageUrl, List<String> amenities) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.imageUrl = imageUrl;
        this.amenities = amenities;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CourtType getType() { return type; }
    public void setType(CourtType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Court court = (Court) o;
        return Objects.equals(id, court.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
