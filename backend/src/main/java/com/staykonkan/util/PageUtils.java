package com.staykonkan.util;

import com.staykonkan.dto.PageRequestDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Converts the API-facing PageRequestDTO (Phase 3 foundation) into Spring
 * Data's Pageable. Centralized so every module's list endpoint (User,
 * Property, Booking, Admin...) builds pagination identically instead of
 * each service reimplementing PageRequest.of(...) with subtly different
 * defaults.
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static Pageable toPageable(PageRequestDTO request) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(request.getPage(), request.getSize(), Sort.by(direction, request.getSortBy()));
    }
}
