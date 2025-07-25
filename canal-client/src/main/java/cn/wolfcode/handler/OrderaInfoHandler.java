package cn.wolfcode.handler;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.redis.SeckillRedisKey;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * Created by wolfcode-lanxw
 */
@Slf4j
@Component
@CanalTable(value = "t_order_info")
public class OrderaInfoHandler implements EntryHandler<OrderInfo> {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 在 Redis 中记录秒杀订单的用户手机号和订单信息。
     * SECKILL_ORDER_SET:1001 -> {12345678901}
     * @param orderInfo
     */
    @Override
    public void insert( OrderInfo orderInfo) {
        log.info("当有数据插入的时候会触发这个方法");
        //在Redis设置Set集合.  存储的是抢到秒杀商品用户的手机号码
        //seckillOrderSet:12 ===> [13088889999,13066668888]
        String orderSetKey = SeckillRedisKey.SECKILL_ORDER_SET.getRealKey(String.valueOf(orderInfo.getSeckillId()));
        redisTemplate.opsForSet().add(orderSetKey,String.valueOf(orderInfo.getUserId()));
        //创建好的订单对象,存储到Redis的hash结构中.
        String orderHashKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(orderHashKey,orderInfo.getOrderNo(), JSON.toJSONString(orderInfo));
    }

    /**
     * 更新 Redis 中的订单信息
     * SECKILL_ORDER_HASH -> {ORDER12345: {"orderNo":"ORDER12345","seckillId":1001,"userId":12345678901}}
     * @param before
     * @param after
     */
    @Override
    public void update(OrderInfo before, OrderInfo after) {
        log.info("当有数据更新的时候会触发这个方法");
        String orderHashKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        redisTemplate.opsForHash().put(orderHashKey,after.getOrderNo(), JSON.toJSONString(after));
    }

    @Override
    public void delete(OrderInfo orderInfo) {
        log.info("当有数据删除的时候会触发这个方法");
    }
}
