package cn.wolfcode.web.controller;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.seckillserver.cache.BloomFilterService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by lanxw
 * 秒杀商品信息查询
 */
@RestController
@RequestMapping("/seckillProduct")
@Slf4j
public class SeckillProductController {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private BloomFilterService bloomFilterService;

    /**
     *  线程 500 循环次数 10
     *  2000/qps
     *  4600/qps
     */
    @RequestMapping("/queryByTime")
    public Result<List<SeckillProductVo>> queryByTime(Integer time){
        return Result.success(seckillProductService.queryByTimeFromCache(time));
    }

    /**
     *
     * 线程 500 循环次数 10
     * 2600/qps
     * 4700/qps
     */
    @RequestMapping("/find")
    public Result<SeckillProductVo> find(Integer time,Long seckillId){
        if (!bloomFilterService.mightContainProductId(String.valueOf(seckillId))) {
            return Result.error(SeckillCodeMsg.PRODUCT_NOT_EXIST);
        }
        return Result.success(seckillProductService.findFromCache(time,seckillId));
    }
}
