package cn.wolfcode.web.feign;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.seckillserver.cache.BloomFilterService;
import cn.wolfcode.service.ISeckillProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by wolfcode-lanxw
 */
@RestController
@RequestMapping("/seckillProduct")
public class SeckillFeignClient {
    @Autowired
    private ISeckillProductService seckillProductService;
    @RequestMapping("/queryByTimeForJob")
    public Result<List<SeckillProductVo>> queryByTimeForJob(@RequestParam("time") Integer time){
        return Result.success(seckillProductService.queryByTime(time));
    }

    @Autowired
    private BloomFilterService bloomFilterService;

    @RequestMapping("/rebuild")
    public String rebuild(@RequestParam Integer time) {
        bloomFilterService.rebuild(time);
        return "布隆过滤器已重建，场次：" + time;
    }
}






