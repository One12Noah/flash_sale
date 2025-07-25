package cn.wolfcode.seckillserver.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class TwoLevelCacheServiceImpl<T> implements TwoLevelCacheService<T> {

    private Cache<String, T> localCache;

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        // 本地缓存最大10000条，默认5分钟过期
        localCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public T get(String key, Class<T> clazz, Supplier<T> loader, long localExpire, long redisExpire) {
        // 1. 本地缓存
        T value = localCache.getIfPresent(key);
        if (value != null) {
            if (NULL_PLACEHOLDER.equals(value)) return null;
            return value;
        }
        /// 2. Redis Hash结构
        // key格式：seckillProductHash:{time}:{seckillId}
        String[] parts = key.split(":");
        if (parts.length == 3 && "seckillProductHash".equals(parts[0])) {
            String hashKey = parts[0] + ":" + parts[1]; // seckillProductHash:{time}
            String field = parts[2]; // seckillId
            Object hashValue = redissonClient.getMap(hashKey).get(field);
            if (hashValue != null) {
                if (NULL_PLACEHOLDER.equals(hashValue)) {
                    localCache.put(key, (T) NULL_PLACEHOLDER);
                    return null;
                }
                T obj;
                if (clazz == String.class) {
                    obj = (T) hashValue.toString();
                } else {
                    obj = com.alibaba.fastjson.JSON.parseObject(hashValue.toString(), clazz);
                }
                localCache.put(key, obj);
                return obj;
            }
        }
        // 3. 数据库
        value = loader.get();
        if (value == null) {
            // 空值缓存
            localCache.put(key, (T) NULL_PLACEHOLDER);
            return null;
        }
        // 正常回填
        localCache.put(key, value);
        return value;
    }

    @Override
    public void put(String key, T value, long localExpire, long redisExpire) {
        localCache.put(key, value);
    }

    @Override
    public void evict(String key) {
        localCache.invalidate(key);
    }

    // 定义空值标记
    private static final String NULL_PLACEHOLDER = "##NULL##";
    private static final long LOCAL_NULL_EXPIRE = 30; // 秒
    private static final long REDIS_NULL_EXPIRE = 300; // 秒
}
