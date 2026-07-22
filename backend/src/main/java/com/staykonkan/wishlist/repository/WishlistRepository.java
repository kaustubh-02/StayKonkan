package com.staykonkan.wishlist.repository;

import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import com.staykonkan.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndProperty(User user, Property property);

    Page<Wishlist> findByUser(User user, Pageable pageable);

    void deleteByUserAndProperty(User user, Property property);

    long countByProperty(Property property);

    // Cascade-cleanup helpers, per the "deleting a property/user should also
    // remove wishlist records" business rule.
    void deleteAllByProperty(Property property);

    void deleteAllByUser(User user);
}
