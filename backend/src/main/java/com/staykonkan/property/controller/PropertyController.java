package com.staykonkan.property.controller;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.property.dto.CreatePropertyRequest;
import com.staykonkan.property.dto.PropertyResponse;
import com.staykonkan.property.dto.UpdatePropertyRequest;
import com.staykonkan.property.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    public PropertyResponse createProperty(
            @Valid @RequestBody CreatePropertyRequest request
    ) {
        return propertyService.createProperty(request);
    }

    @PutMapping("/{id}")
    public PropertyResponse updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        return propertyService.updateProperty(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
    }

    @GetMapping("/{id}")
    public PropertyResponse getProperty(@PathVariable Long id) {
        return propertyService.getPropertyById(id);
    }

    @GetMapping
    public PageResponseDTO<PropertyResponse> getAllProperties(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDir

    ) {

        return propertyService.getAllProperties(
                page,
                size,
                sortBy,
                sortDir
        );
    }

    @GetMapping("/search")
    public PageResponseDTO<PropertyResponse> searchByLocation(

            @RequestParam String location,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size

    ) {

        return propertyService.searchByLocation(
                location,
                page,
                size
        );
    }

}