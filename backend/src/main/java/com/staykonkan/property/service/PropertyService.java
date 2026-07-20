package com.staykonkan.property.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.property.dto.CreatePropertyRequest;
import com.staykonkan.property.dto.PropertyResponse;
import com.staykonkan.property.dto.UpdatePropertyRequest;

public interface PropertyService {

    PropertyResponse createProperty(CreatePropertyRequest request);

    PropertyResponse updateProperty(Long id, UpdatePropertyRequest request);

    void deleteProperty(Long id);

    PropertyResponse getPropertyById(Long id);

    PageResponseDTO<PropertyResponse> getAllProperties(
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    PageResponseDTO<PropertyResponse> searchByLocation(
            String location,
            int page,
            int size
    );
}