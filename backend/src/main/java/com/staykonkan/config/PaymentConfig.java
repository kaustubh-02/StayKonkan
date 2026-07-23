package com.staykonkan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Platform-level payment settings, distinct from gateway credentials
 * (see {@link RazorpayConfig}). The commission percentage in particular
 * must never be a hardcoded constant in business logic — see
 * app.payment.platform-commission-percentage in application.properties,
 * consumed by PaymentServiceImpl.
 */
@Configuration
public class PaymentConfig {

    @ConfigurationProperties(prefix = "app.payment")
    @Bean
    public PaymentProperties paymentProperties() {
        return new PaymentProperties();
    }

    public static class PaymentProperties {

        private BigDecimal platformCommissionPercentage;
        private String defaultCurrency;

        public BigDecimal getPlatformCommissionPercentage() {
            return platformCommissionPercentage;
        }

        public void setPlatformCommissionPercentage(BigDecimal platformCommissionPercentage) {
            this.platformCommissionPercentage = platformCommissionPercentage;
        }

        public String getDefaultCurrency() {
            return defaultCurrency;
        }

        public void setDefaultCurrency(String defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
        }
    }
}
