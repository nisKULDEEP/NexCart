package com.example.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Redis Cache Configuration
 * Enables caching support using Redis as the cache backend
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Spring will automatically configure Redis caching based on application.yml
    // TTL and connection pooling are configured in the service YAML
}

