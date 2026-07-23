package com.staykonkan.refund.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.refund.dto.RefundRequest;
import com.staykonkan.refund.dto.RefundResponse;

public interface RefundService {

    RefundResponse requestRefund(Long bookingId, RefundRequest request);

    RefundResponse getRefundByBooking(Long bookingId);

    PageResponseDTO<RefundResponse> getAllRefunds(int page, int size);
}
