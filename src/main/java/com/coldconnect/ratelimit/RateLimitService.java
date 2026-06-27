package com.coldconnect.ratelimit;

import com.coldconnect.exception.AppException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final int authRequestsPerMinute;
    private final int apiRequestsPerMinute;

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    public RateLimitService(
            @Value("${rate-limit.auth.requests-per-minute}") int authRequestsPerMinute,
            @Value("${rate-limit.api.requests-per-minute}") int apiRequestsPerMinute) {
        this.authRequestsPerMinute = authRequestsPerMinute;
        this.apiRequestsPerMinute = apiRequestsPerMinute;
    }

    public void checkAuthLimit(String ip) {
        Bucket bucket = authBuckets.computeIfAbsent(ip, k -> newBucket(authRequestsPerMinute));
        if (!bucket.tryConsume(1)) {
            throw new AppException.TooManyRequestsException(
                    "Too many requests. Auth allows " + authRequestsPerMinute + " req/min.");
        }
    }

    public void checkApiLimit(String ip) {
        Bucket bucket = apiBuckets.computeIfAbsent(ip, k -> newBucket(apiRequestsPerMinute));
        if (!bucket.tryConsume(1)) {
            throw new AppException.TooManyRequestsException(
                    "Too many requests. API allows " + apiRequestsPerMinute + " req/min.");
        }
    }

    private Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute,
                        Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }
}
