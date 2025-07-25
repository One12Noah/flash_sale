package cn.wolfcode.web.feign.fallback;

import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.web.feign.SeckillProductFeignAPI;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by wolfcode-lanxw
 */
@Component
public class SeckillProductFeignFallback implements SeckillProductFeignAPI {
    @Override
    public Result<List<SeckillProductVo>> queryByTimeForJob(Integer time) {
        return null;
    }
    @Override
    public String rebuild(Integer time) {
        return null;
    }
}
