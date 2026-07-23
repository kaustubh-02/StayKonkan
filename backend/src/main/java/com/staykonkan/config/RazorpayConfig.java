package com.staykonkan.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Razorpay SDK client as a Spring bean, same pattern as
 * {@link CloudinaryConfig}. Credentials come from environment variables
 * (see application.properties: app.razorpay.*) and are never hardcoded
 * or logged.
 * <p>
 * No @NotBlank validation on the properties, deliberately: the
 * application must still start cleanly in dev/CI before real Razorpay
 * credentials are provisioned. {@code RazorpayClient}'s constructor does
 * not make a network call, so empty credentials don't fail startup —
 * they only fail the first real API call, which surfaces as a normal
 * {@code ExternalServiceException} (502) from RazorpayPaymentService.
 */
@Configuration
public class RazorpayConfig {

    @ConfigurationProperties(prefix = "app.razorpay")
    @Bean
    public RazorpayProperties razorpayProperties() {
        return new RazorpayProperties();
    }

    @Bean
    public RazorpayClient razorpayClient(RazorpayProperties properties) throws RazorpayException {
        return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
    }

    public static class RazorpayProperties {

        private String keyId;
        private String keySecret;

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeySecret() {
            return keySecret;
        }

        public void setKeySecret(String keySecret) {
            this.keySecret = keySecret;
        }
    }
}
