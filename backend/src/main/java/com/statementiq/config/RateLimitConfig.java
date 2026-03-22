package com.statementiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.free-uploads-per-day:5}")
    private int freeUploadsPerDay;

    @Value("${rate-limit.pro-uploads-per-day:30}")
    private int proUploadsPerDay;

    public int getFreeUploadsPerDay() {
        return freeUploadsPerDay;
    }

    public int getProUploadsPerDay() {
        return proUploadsPerDay;
    }
}
