package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.Product;
import cn.wolfcode.domain.SeckillProduct;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.mapper.SeckillProductMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.seckillserver.cache.TwoLevelCacheService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.feign.ProductFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by lanxw
 */
@Service
public class SeckillProductServiceImpl implements ISeckillProductService {
    @Autowired
    private SeckillProductMapper seckillProductMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private ProductFeignApi productFeignApi;
    @Autowired
    private TwoLevelCacheService<String> twoLevelCacheService;
    @Override
    public List<SeckillProductVo> queryByTime(Integer time) {
        //1.查询秒杀商品集合数据(场次查询当天的数据)
        List<SeckillProduct> seckillProductList = seckillProductMapper.queryCurrentlySeckillProduct(time);
        if(seckillProductList.size()==0){
            return Collections.EMPTY_LIST;
        }
        //2.遍历秒杀商品集合数据,获取商品ID集合
        List<Long> productIds = new ArrayList<>();
        for(SeckillProduct seckillProduct:seckillProductList){
            productIds.add(seckillProduct.getProductId());
        }
        //3.远程调用,获取商品集合
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
        if(result==null || result.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        List<Product> productList = result.getData();
        //定义productId和Product对象的映射关系
        Map<Long,Product> productMap = new HashMap<>();
        for(Product product:productList){
            productMap.put(product.getId(),product);
        }
        //4.将商品和秒杀商品数据集合,封装Vo并返回
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for(SeckillProduct seckillProduct:seckillProductList){
            SeckillProductVo vo = new SeckillProductVo();
            //把seckillProduct的数据和Product的数据封装到Vo
            Product product = productMap.get(seckillProduct.getProductId());
            //将数据拷贝到vo对象中
            BeanUtils.copyProperties(product,vo);
            BeanUtils.copyProperties(seckillProduct,vo);
            vo.setCurrentCount(seckillProduct.getStockCount());//当前数据默认等于库存数量.
            seckillProductVoList.add(vo);
        }
        return seckillProductVoList;
    }

    @Override
    public SeckillProductVo find(Integer time, Long seckillId) {
        //查询秒杀商品对象
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        //根据id查询商品对象
        List<Long> productIds = new ArrayList<>();
        productIds.add(seckillProduct.getProductId());
        Result<List<Product>> result = productFeignApi.queryByIds(productIds);
        if(result==null || result.hasError()){
            throw new BusinessException(SeckillCodeMsg.PRODUCT_SERVER_ERROR);
        }
        Product product = result.getData().get(0);
        //将数据封装成vo对象
        SeckillProductVo vo = new SeckillProductVo();
        BeanUtils.copyProperties(product,vo);
        BeanUtils.copyProperties(seckillProduct,vo);
        vo.setCurrentCount(seckillProduct.getStockCount());
        return vo;
    }

    @Override
    public int decrStockCount(Long seckillId) {
        return seckillProductMapper.decrStock(seckillId);
    }

    /**
     * 从 Redis 缓存中获取指定时间段的秒杀商品列表，并将其转换为 SeckillProductVo 对象的集合返回
     * @param time
     * @return
     */
    @Override
    public List<SeckillProductVo> queryByTimeFromCache(Integer time) {
        String key = SeckillRedisKey.SECKILL_PRODUCT_HASH.getRealKey(String.valueOf(time));
        //返回的是该哈希下所有 value（即所有商品的 JSON 字符串），不包含 field。
        List<Object> objStrList = redisTemplate.opsForHash().values(key);
        List<SeckillProductVo> seckillProductVoList = new ArrayList<>();
        for(Object objStr:objStrList){
            seckillProductVoList.add(JSON.parseObject((String)objStr,SeckillProductVo.class));
        }
        return seckillProductVoList;
    }

    /**
     * 从 Redis 缓存中获取指定时间段和秒杀商品 ID 对应的秒杀商品详情，
     * 并将其转换为 SeckillProductVo 对象返回
     * @param time
     * @param seckillId
     * @return
     */
    @Override
    public SeckillProductVo findFromCache(Integer time, Long seckillId) {
        String key = "seckillProductHash:" + time + ":" + seckillId;
        String json = twoLevelCacheService.get(
            key,
            String.class,
            () -> {
                // 数据库兜底
                SeckillProductVo vo = seckillProductMapper.findVoByTimeAndId(time, seckillId);
                return vo == null ? null : JSON.toJSONString(vo);
            },
            60, // 本地缓存60秒
            300 // Redis空值缓存5分钟
        );
        if (json == null) return null;
        return JSON.parseObject(json, SeckillProductVo.class);
    }

    /**
     * 将秒杀商品的库存从数据库同步到 Redis 中
     * SECKILL_STOCK_COUNT_HASH:1 -> {1001: 10}
     * @param time
     * @param seckillId
     */
    @Override
    public void syncStockToRedis(Integer time, Long seckillId) {
        SeckillProduct seckillProduct = seckillProductMapper.find(seckillId);
        if(seckillProduct.getStockCount()>0){
            // orderStockCount:10
            String key = SeckillRedisKey.SECKILL_STOCK_COUNT_HASH.getRealKey(String.valueOf(time));
            //将数据库库存同步到Redis中.
            redisTemplate.opsForHash().put(key,String.valueOf(seckillId),String.valueOf(seckillProduct.getStockCount()));
        }
    }

    @Override
    public void incrStockCount(Long seckillId) {
        seckillProductMapper.incrStock(seckillId);
    }


}
