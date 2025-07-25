package cn.wolfcode.common.aop;

import cn.wolfcode.common.anno.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RateLimiterAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        String key = rateLimiter.key();
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        // 设置速率（只会在第一次设置，后续不会重复设置）
        limiter.trySetRate(RateType.OVERALL, rateLimiter.permitsPerSecond(), 1, RateIntervalUnit.SECONDS);
        boolean canProceed = limiter.tryAcquire(1, rateLimiter.timeout(), rateLimiter.timeUnit());
        if (!canProceed) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }
}