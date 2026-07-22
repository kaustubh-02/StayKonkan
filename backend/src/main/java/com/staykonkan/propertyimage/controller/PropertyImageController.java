package com.staykonkan.propertyimage.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.propertyimage.dto.PropertyImageResponse;
import com.staykonkan.propertyimage.dto.UpdateImageOrderRequest;
import com.staykonkan.propertyimage.dto.UploadPropertyImageRequest;
import com.staykonkan.propertyimage.service.PropertyImageService;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Property image endpoints. Ownership (property owner or ADMIN) is
 * enforced inside PropertyImageServiceImpl per image/property, matching
 * how Booking/Review/Wishlist push per-record checks into the service
 * layer rather than relying on a blanket @PreAuthorize here.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/property-images")
@Tag(name = "Property Images", description = "Upload, order, and manage property images (Cloudinary-backed)")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    public PropertyImageController(PropertyImageService propertyImageService) {
        this.propertyImageService = propertyImageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload an image for a property",
            description = "Only the property owner or an admin may upload. Max 10 images per property. " +
                    "The first image uploaded automatically becomes the cover image.")
    public ApiResponse<PropertyImageResponse> uploadImage(
            @RequestParam Long propertyId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Boolean isCover) {

        UploadPropertyImageRequest request = new UploadPropertyImageRequest();
        request.setPropertyId(propertyId);
        request.setFile(file);
        request.setIsCover(isCover);

        return ApiResponse.ok(propertyImageService.uploadImage(request), "Image uploaded successfully");
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete a property image",
            description = "Only the property owner or an admin may delete. If the deleted image was the " +
                    "cover, the next remaining image (by display order) automatically becomes cover.")
    public ApiResponse<Void> deleteImage(@PathVariable Long imageId) {
        propertyImageService.deleteImage(imageId);
        return ApiResponse.message("Image deleted successfully");
    }

    @PutMapping("/cover/{imageId}")
    @Operation(summary = "Set an image as the property's cover image")
    public ApiResponse<PropertyImageResponse> setCoverImage(@PathVariable Long imageId) {
        return ApiResponse.ok(propertyImageService.setCoverImage(imageId), "Cover image updated successfully");
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder a property's images",
            description = "All images in the request must belong to the same property")
    public ApiResponse<List<PropertyImageResponse>> reorderImages(@Valid @RequestBody UpdateImageOrderRequest request) {
        return ApiResponse.ok(propertyImageService.reorderImages(request), "Image order updated successfully");
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get all images for a property, ordered by display order")
    public ApiResponse<List<PropertyImageResponse>> getImagesByProperty(@PathVariable Long propertyId) {
        return ApiResponse.ok(propertyImageService.getImagesByProperty(propertyId));
    }
}
