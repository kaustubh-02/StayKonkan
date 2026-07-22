package com.staykonkan.amenity.entity;

import com.staykonkan.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable, catalog-level amenity (e.g. "WiFi", "Swimming Pool").
 * Deliberately independent of any single Property — the many-to-many
 * relationship lives on {@link com.staykonkan.property.entity.Property}
 * so the catalog can be managed centrally by ADMIN while owners just
 * pick from it.
 */
@Entity
@Table(name = "amenities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amenity extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AmenityCategory category = AmenityCategory.OTHER;

    // Soft-delete flag, same principle as User.status = DELETED: an amenity
    // already assigned to existing properties must never be hard-deleted,
    // it's just hidden from future selection (active = false).
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
