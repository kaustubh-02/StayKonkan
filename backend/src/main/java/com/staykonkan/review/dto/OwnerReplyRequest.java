package com.staykonkan.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerReplyRequest {

    @NotBlank(message = "Reply must not be blank")
    @Size(max = 2000, message = "Reply must not exceed 2000 characters")
    private String reply;
}
