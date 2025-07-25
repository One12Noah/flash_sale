package cn.wolfcode.seckillserver.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Calendar;
import cn.wolfcode.mapper.SeckillProductMapper;

@Service
public class BloomFilterService {

    private BloomFilter<String> productBloomFilter;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SeckillProductMapper seckillProductMapper;

    // 初始化时可加载默认场次
    @PostConstruct
    public void init() {
        // 例如默认加载当前时间段
        Integer currentTime = getCurrentTimeSlot();
        doBuild(currentTime);
    }

    /**
     * 对外暴露的重建方法，支持传入场次
     */
    public synchronized void rebuild(Integer time) {
        doBuild(time);
    }

    /**
     * 核心构建逻辑
     */
    private void doBuild(Integer time) {
        List<String> allProductIds = loadProductIdsByTimeFromCache(time);
        if (allProductIds == null || allProductIds.isEmpty()) {
            allProductIds = loadProductIdsByTimeFromDB(time);
        }
        productBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                allProductIds.size() == 0 ? 1000 : allProductIds.size(),
                0.001
        );
        for (String id : allProductIds) {
            productBloomFilter.put(id);
        }
    }

    // 从Redis缓存获取指定场次商品ID
    private List<String> loadProductIdsByTimeFromCache(Integer time) {
        String key = "seckillProductHash:" + time;
        Set<Object> fields = redisTemplate.opsForHash().keys(key);
        List<String> ids = new ArrayList<>();
        for (Object field : fields) {
            ids.add(String.valueOf(field));
        }
        return ids;
    }

    // 从数据库获取指定场次商品ID
    private List<String> loadProductIdsByTimeFromDB(Integer time) {
        return seckillProductMapper.selectProductIdsByTime(time);
    }

    // 获取当前时间段（场次），可根据实际业务实现
    private Integer getCurrentTimeSlot() {
        // 例如：根据当前小时返回场次编号
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        // 你的业务场次映射逻辑
        if (hour >= 6 && hour < 12) return 6;
        if (hour >= 12 && hour < 18) return 12;
        if (hour >= 18 && hour < 24) return 18;
        return 0;
    }

    public boolean mightContainProductId(String productId) {
        return productBloomFilter.mightContain(productId);
    }

    // 新增商品时可调用此方法动态添加
    public void addProductId(String productId) {
        productBloomFilter.put(productId);
    }
}