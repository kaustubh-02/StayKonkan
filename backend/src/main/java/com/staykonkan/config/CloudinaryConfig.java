package com.staykonkan.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary is wired here at configuration level ONLY, per the Phase 3
 * scope ("Cloudinary — Configuration Only"). No upload/delete/transform
 * logic exists yet — that arrives with the Media module in a later
 * phase, which will inject the {@link Cloudinary} bean produced below
 * instead of constructing its own client.
 *
 * Credentials are read from environment variables (see
 * application.properties: app.cloudinary.*) and are never hardcoded.
 */
@Configuration
public class CloudinaryConfig {

    @ConfigurationProperties(prefix = "app.cloudinary")
    @Bean
    public CloudinaryProperties cloudinaryProperties() {
        return new CloudinaryProperties();
    }

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
    }

    public static class CloudinaryProperties {

        // No @NotBlank here deliberately: Cloudinary is configuration-only
        // in this phase (no Media module consumes it yet), so the
        // application must still start cleanly in dev/CI even before
        // real Cloudinary credentials are provisioned. The Media module
        // (later phase) is responsible for failing loudly if it tries to
        // upload with empty credentials.
        private String cloudName;
        private String apiKey;
        private String apiSecret;

        public String getCloudName() {
            return cloudName;
        }

        public void setCloudName(String cloudName) {
            this.cloudName = cloudName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
    }
}
