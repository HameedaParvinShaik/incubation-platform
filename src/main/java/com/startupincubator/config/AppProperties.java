package com.startupincubator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Upload upload = new Upload();
    private Email email = new Email();
    private Startup startup = new Startup();
    private Pagination pagination = new Pagination();
    private Security security = new Security();
    private Jwt jwt = new Jwt();
    
    @Data
    public static class Upload {
        private String dir = "uploads/";
        private String tempDir = "uploads/temp/";
        private String documentsDir = "uploads/documents/";
        private String profilePicturesDir = "uploads/profiles/";
    }
    
    @Data
    public static class Email {
        private String from = "noreply@startupincubator.com";
        private String support = "support@startupincubator.com";
    }
    
    @Data
    public static class Startup {
        private String statusDefault = "PENDING";
        private int maxTeamMembers = 10;
    }
    
    @Data
    public static class Pagination {
        private int defaultPage = 0;
        private int defaultSize = 10;
    }
    
    @Data
    public static class Security {
        private int bcryptStrength = 12;
    }
    
    @Data
    public static class Jwt {
        private String secret = "incubationPlatformJwtSecretKey2024Secure";
        private long expirationMs = 86400000;
    }
}