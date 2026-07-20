package com.staykonkan.exception;

import com.staykonkan.constant.ErrorCode;

/**
 * Thrown when a downstream integration (Cloudinary, SMS/WhatsApp provider,
 * future payment gateway) fails. Maps to HTTP 502 — signals to the client
 * that the failure is upstream, not caused by their request.
 */
public class ExternalServiceException extends StayKonkanException {

    public ExternalServiceException(String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message, cause);
    }
}
