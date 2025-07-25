package cn.wolfcode.common.web.interceptor;

import cn.wolfcode.common.constants.CommonConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Created by lanxw
 */
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        template.header(CommonConstants.FEIGN_REQUEST_KEY,CommonConstants.FEIGN_REQUEST_TRUE);
    }
}
/**
 * 此拦截器的主要作用是为 Feign 请求添加标识头部，便于后续处理。
 */
