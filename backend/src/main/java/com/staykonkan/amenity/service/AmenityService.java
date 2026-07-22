package com.staykonkan.amenity.service;

import com.staykonkan.amenity.dto.AmenityResponse;
import com.staykonkan.amenity.dto.AssignAmenitiesRequest;
import com.staykonkan.amenity.dto.CreateAmenityRequest;
import com.staykonkan.amenity.dto.UpdateAmenityRequest;
import com.staykonkan.amenity.entity.AmenityCategory;

import java.util.List;

public interface AmenityService {

    AmenityResponse createAmenity(CreateAmenityRequest request);

    AmenityResponse updateAmenity(Long amenityId, UpdateAmenityRequest request);

    void deleteAmenity(Long amenityId);

    List<AmenityResponse> getAllAmenities();

    List<AmenityResponse> getAmenitiesByCategory(AmenityCategory category);

    List<AmenityResponse> assignAmenitiesToProperty(Long propertyId, AssignAmenitiesRequest request);

    void removeAmenityFromProperty(Long propertyId, Long amenityId);

    List<AmenityResponse> getPropertyAmenities(Long propertyId);
}
