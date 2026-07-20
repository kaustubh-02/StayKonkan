package com.staykonkan.dto;

import com.staykonkan.constant.AppConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PageRequestDTO {

    @Builder.Default
    @Min(0)
    private int page = AppConstants.DEFAULT_PAGE_NUMBER;

    @Builder.Default
    @Min(1)
    @Max(AppConstants.MAX_PAGE_SIZE)
    private int size = AppConstants.DEFAULT_PAGE_SIZE;

    @Builder.Default
    private String sortBy = AppConstants.DEFAULT_SORT_FIELD;

    @Builder.Default
    private String sortDirection = AppConstants.DEFAULT_SORT_DIRECTION;
}
