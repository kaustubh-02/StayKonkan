package com.staykonkan.property.service.impl;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.property.dto.CreatePropertyRequest;
import com.staykonkan.property.dto.PropertyResponse;
import com.staykonkan.property.dto.UpdatePropertyRequest;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.mapper.PropertyMapper;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.property.service.PropertyService;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;

    public PropertyServiceImpl(
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            PropertyMapper propertyMapper
    ) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.propertyMapper = propertyMapper;
    }

    @Override
    public PropertyResponse createProperty(CreatePropertyRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        SecurityUserPrincipal principal =
                (SecurityUserPrincipal) authentication.getPrincipal();

        User owner = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Property property = propertyMapper.toEntity(request, owner);

        property = propertyRepository.save(property);

        return propertyMapper.toResponse(property);
    }

    @Override
    public PropertyResponse updateProperty(Long id, UpdatePropertyRequest request) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (request.getTitle() != null)
            property.setTitle(request.getTitle());

        if (request.getDescription() != null)
            property.setDescription(request.getDescription());

        if (request.getLocation() != null)
            property.setLocation(request.getLocation());

        if (request.getAddress() != null)
            property.setAddress(request.getAddress());

        if (request.getLatitude() != null)
            property.setLatitude(request.getLatitude());

        if (request.getLongitude() != null)
            property.setLongitude(request.getLongitude());

        if (request.getPricePerNight() != null)
            property.setPricePerNight(request.getPricePerNight());

        if (request.getMaxGuests() != null)
            property.setMaxGuests(request.getMaxGuests());

        if (request.getBedrooms() != null)
            property.setBedrooms(request.getBedrooms());

        if (request.getBathrooms() != null)
            property.setBathrooms(request.getBathrooms());

        if (request.getAmenities() != null)
            property.setAmenities(request.getAmenities());

        if (request.getImageUrls() != null)
            property.setImageUrls(request.getImageUrls());

        property = propertyRepository.save(property);

        return propertyMapper.toResponse(property);
    }

    @Override
    public void deleteProperty(Long id) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        propertyRepository.delete(property);
    }

    @Override
    public PropertyResponse getPropertyById(Long id) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        return propertyMapper.toResponse(property);
    }

    @Override
    public PageResponseDTO<PropertyResponse> getAllProperties(
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<Property> propertyPage =
                propertyRepository.findAll(PageRequest.of(page, size, sort));

        Page<PropertyResponse> responsePage =
                propertyPage.map(propertyMapper::toResponse);

        return PageResponseDTO.from(responsePage);
    }

    @Override
    public PageResponseDTO<PropertyResponse> searchByLocation(
            String location,
            int page,
            int size
    ) {

        Page<Property> propertyPage =
                propertyRepository.findByLocationContainingIgnoreCase(
                        location,
                        PageRequest.of(page, size)
                );

        Page<PropertyResponse> responsePage =
                propertyPage.map(propertyMapper::toResponse);

        return PageResponseDTO.from(responsePage);
    }
}