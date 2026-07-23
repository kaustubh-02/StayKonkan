package com.staykonkan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Holds the Razorpay webhook signing secret. Deliberately a separate
 * config class from {@link RazorpayConfig} (Module 10A) rather than
 * adding a field there — Module 10B must not modify Module 10A files.
 * The webhook secret uses a different signing scheme from the checkout
 * key-secret (see {@link com.staykonkan.webhook.security.WebhookSignatureVerifier}),
 * so keeping it in its own properties class also avoids conflating the
 * two secrets' distinct purposes.
 */
@Configuration
public class RazorpayWebhookConfig {

    @ConfigurationProperties(prefix = "app.razorpay.webhook")
    @Bean
    public RazorpayWebhookProperties razorpayWebhookProperties() {
        return new RazorpayWebhookProperties();
    }

    public static class RazorpayWebhookProperties {

        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
