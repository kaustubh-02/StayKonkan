package com.staykonkan.property.mapper;

import com.staykonkan.property.dto.CreatePropertyRequest;
import com.staykonkan.property.dto.PropertyResponse;
import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PropertyMapper {

    public Property toEntity(CreatePropertyRequest request, User owner) {

        return Property.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .pricePerNight(request.getPricePerNight())
                .maxGuests(request.getMaxGuests())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .amenities(request.getAmenities())
                .imageUrls(request.getImageUrls())
                .owner(owner)
                .build();
    }

    public PropertyResponse toResponse(Property property) {

        PropertyResponse response = new PropertyResponse();

        response.setId(property.getId());
        response.setTitle(property.getTitle());
        response.setDescription(property.getDescription());
        response.setLocation(property.getLocation());
        response.setAddress(property.getAddress());
        response.setLatitude(property.getLatitude());
        response.setLongitude(property.getLongitude());
        response.setPricePerNight(property.getPricePerNight());
        response.setMaxGuests(property.getMaxGuests());
        response.setBedrooms(property.getBedrooms());
        response.setBathrooms(property.getBathrooms());
        response.setAmenities(property.getAmenities());
        response.setImageUrls(property.getImageUrls());
        response.setPropertyStatus(property.getPropertyStatus());

        if (property.getOwner() != null) {
            response.setOwnerId(property.getOwner().getId());
            response.setOwnerName(property.getOwner().getFullName());
        }

        response.setCreatedAt(property.getCreatedAt());
        response.setUpdatedAt(property.getUpdatedAt());

        return response;
    }
}