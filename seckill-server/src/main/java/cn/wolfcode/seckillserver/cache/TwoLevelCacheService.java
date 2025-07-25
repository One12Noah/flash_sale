package cn.wolfcode.seckillserver.cache;

import java.util.function.Supplier;

public interface TwoLevelCacheService<T> {
    T get(String key, Class<T> clazz, Supplier<T> loader, long localExpire, long redisExpire);
    void put(String key, T value, long localExpire, long redisExpire);
    void evict(String key);
}
