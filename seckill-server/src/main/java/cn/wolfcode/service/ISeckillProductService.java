package cn.wolfcode.service;

import cn.wolfcode.domain.SeckillProductVo;

import java.util.List;

/**
 * Created by lanxw
 */
public interface ISeckillProductService {
    /**
     * 查询秒杀列表的数据
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTime(Integer time);

    /**
     * 根据秒杀场次和秒杀商品ID查询秒杀商品VO对象
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo find(Integer time, Long seckillId);

    /**
     * 根据秒杀商品ID扣减库存
     * @param seckillId
     */
    int decrStockCount(Long seckillId);

    /**
     * 从缓存中获取秒杀商品列表集合
     * @param time
     * @return
     */
    List<SeckillProductVo> queryByTimeFromCache(Integer time);

    /**
     * 从缓存中获取秒杀商品详情
     * @param time
     * @param seckillId
     * @return
     */
    SeckillProductVo findFromCache(Integer time, Long seckillId);

    /**
     * 查询数据库库存同步到Redis中
     * @param time
     * @param seckillId
     */
    void syncStockToRedis(Integer time, Long seckillId);

    /**
     * 增加库存
     * @param seckillId
     */
    void incrStockCount(Long seckillId);
}
