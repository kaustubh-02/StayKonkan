package com.staykonkan.wishlist.entity;

import com.staykonkan.entity.AuditableEntity;
import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "wishlists",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wishlist_user_property",
                columnNames = {"user_id", "property_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
