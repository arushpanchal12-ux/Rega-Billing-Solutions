package com.regabilling.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket createRateLimitBucket() {
        // Allow 1000 requests per minute per instance
        Bandwidth limit = Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
