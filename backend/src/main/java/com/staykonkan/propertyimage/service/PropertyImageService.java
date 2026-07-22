package com.staykonkan.propertyimage.service;

import com.staykonkan.propertyimage.dto.PropertyImageResponse;
import com.staykonkan.propertyimage.dto.UpdateImageOrderRequest;
import com.staykonkan.propertyimage.dto.UploadPropertyImageRequest;

import java.util.List;

public interface PropertyImageService {

    PropertyImageResponse uploadImage(UploadPropertyImageRequest request);

    void deleteImage(Long imageId);

    PropertyImageResponse setCoverImage(Long imageId);

    List<PropertyImageResponse> reorderImages(UpdateImageOrderRequest request);

    List<PropertyImageResponse> getImagesByProperty(Long propertyId);
}
