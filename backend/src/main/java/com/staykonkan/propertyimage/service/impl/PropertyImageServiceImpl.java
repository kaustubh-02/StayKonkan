package com.staykonkan.propertyimage.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.staykonkan.exception.ExternalServiceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.propertyimage.dto.PropertyImageResponse;
import com.staykonkan.propertyimage.dto.UpdateImageOrderRequest;
import com.staykonkan.propertyimage.dto.UploadPropertyImageRequest;
import com.staykonkan.propertyimage.entity.PropertyImage;
import com.staykonkan.propertyimage.mapper.PropertyImageMapper;
import com.staykonkan.propertyimage.repository.PropertyImageRepository;
import com.staykonkan.propertyimage.service.PropertyImageService;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PropertyImageServiceImpl implements PropertyImageService {

    private static final int MAX_IMAGES_PER_PROPERTY = 10;
    private static final String CLOUDINARY_FOLDER = "staykonkan/properties";

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyImageMapper propertyImageMapper;
    private final Cloudinary cloudinary;

    @Override
    public PropertyImageResponse uploadImage(UploadPropertyImageRequest request) {

        User currentUser = getCurrentUser();

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Property", request.getPropertyId()));

        assertOwnerOrAdmin(property, currentUser);

        long existingCount = propertyImageRepository.countByProperty(property);

        if (existingCount >= MAX_IMAGES_PER_PROPERTY) {
            throw new ValidationException(
                    "A property can have at most " + MAX_IMAGES_PER_PROPERTY + " images");
        }

        MultipartFile file = request.getFile();

        if (file == null || file.isEmpty()) {
            throw new ValidationException("Image file is required");
        }

        Map<?, ?> uploadResult = uploadToCloudinary(file);

        String imageUrl = String.valueOf(uploadResult.get("secure_url"));
        String publicId = String.valueOf(uploadResult.get("public_id"));

        boolean makeCover = Boolean.TRUE.equals(request.getIsCover()) || existingCount == 0;

        if (makeCover) {
            propertyImageRepository.findByPropertyAndIsCoverTrue(property)
                    .ifPresent(current -> current.setIsCover(false));
        }

        PropertyImage image = PropertyImage.builder()
                .property(property)
                .imageUrl(imageUrl)
                .publicId(publicId)
                .isCover(makeCover)
                .displayOrder((int) existingCount)
                .build();

        image = propertyImageRepository.save(image);

        return propertyImageMapper.toResponse(image);
    }

    @Override
    public void deleteImage(Long imageId) {

        PropertyImage image = findByIdOrThrow(imageId);
        User currentUser = getCurrentUser();

        assertOwnerOrAdmin(image.getProperty(), currentUser);

        Property property = image.getProperty();
        boolean wasCover = Boolean.TRUE.equals(image.getIsCover());

        deleteFromCloudinary(image.getPublicId());

        propertyImageRepository.delete(image);

        if (wasCover) {
            propertyImageRepository.findByPropertyOrderByDisplayOrderAsc(property)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> next.setIsCover(true));
        }
    }

    @Override
    public PropertyImageResponse setCoverImage(Long imageId) {

        PropertyImage image = findByIdOrThrow(imageId);
        User currentUser = getCurrentUser();

        assertOwnerOrAdmin(image.getProperty(), currentUser);

        propertyImageRepository.findByPropertyAndIsCoverTrue(image.getProperty())
                .ifPresent(current -> current.setIsCover(false));

        image.setIsCover(true);

        return propertyImageMapper.toResponse(image);
    }

    @Override
    public List<PropertyImageResponse> reorderImages(UpdateImageOrderRequest request) {

        User currentUser = getCurrentUser();

        List<Long> imageIds = request.getImages().stream()
                .map(UpdateImageOrderRequest.ImageOrderEntry::getImageId)
                .toList();

        List<PropertyImage> images = propertyImageRepository.findAllById(imageIds);

        if (images.size() != imageIds.size()) {
            throw new ResourceNotFoundException("One or more images were not found");
        }

        Set<Long> propertyIds = images.stream()
                .map(img -> img.getProperty().getId())
                .collect(Collectors.toSet());

        if (propertyIds.size() > 1) {
            throw new ValidationException("All images in a reorder request must belong to the same property");
        }

        Property property = images.get(0).getProperty();
        assertOwnerOrAdmin(property, currentUser);

        Map<Long, Integer> orderByImageId = request.getImages().stream()
                .collect(Collectors.toMap(
                        UpdateImageOrderRequest.ImageOrderEntry::getImageId,
                        UpdateImageOrderRequest.ImageOrderEntry::getDisplayOrder
                ));

        images.forEach(image -> image.setDisplayOrder(orderByImageId.get(image.getId())));

        return propertyImageRepository.findByPropertyOrderByDisplayOrderAsc(property)
                .stream()
                .map(propertyImageMapper::toResponse)
                .toList();
    }

    @Override
    public List<PropertyImageResponse> getImagesByProperty(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        return propertyImageRepository.findByPropertyOrderByDisplayOrderAsc(property)
                .stream()
                .map(propertyImageMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PropertyImage findByIdOrThrow(Long imageId) {
        return propertyImageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.of("PropertyImage", imageId));
    }

    private void assertOwnerOrAdmin(Property property, User currentUser) {
        boolean isOwner = property.getOwner().getId().equals(currentUser.getId());
        if (!isOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the property owner or an admin can manage images for this property");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> uploadToCloudinary(MultipartFile file) {
        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", CLOUDINARY_FOLDER)
            );
        } catch (IOException e) {
            throw new ExternalServiceException("Failed to upload image to Cloudinary", e);
        }
    }

    private void deleteFromCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new ExternalServiceException("Failed to delete image from Cloudinary", e);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
