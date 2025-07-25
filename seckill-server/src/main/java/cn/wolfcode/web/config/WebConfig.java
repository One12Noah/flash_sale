package cn.wolfcode.web.config;

import cn.wolfcode.common.web.interceptor.FeignRequestInterceptor;
import cn.wolfcode.common.web.interceptor.RequireLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Created by lanxw
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 创建登录拦截器Bean
    @Bean
    public RequireLoginInterceptor requireLoginInterceptor(StringRedisTemplate redisTemplate){
        return new RequireLoginInterceptor(redisTemplate);
    }
    @Bean
    // 创建Feign专用拦截器Bean：它只作用于 Feign 作为客户端发出的请求，不会拦截外部发到本服务的 HTTP 请求
    public FeignRequestInterceptor feignRequestInterceptor(){
        return new FeignRequestInterceptor();
    }
    // 注入登录拦截器
    @Autowired
    private RequireLoginInterceptor requireLoginInterceptor;
    // 配置拦截器链
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireLoginInterceptor)
                .addPathPatterns("/**");
    }
}
/**
 * RequireLoginInterceptor 需要加到 addInterceptor，因为它拦截的是外部 HTTP 请求。
 * FeignRequestInterceptor 只作用于 Feign 客户端，不属于 Spring MVC 拦截器链，无需加到 addInterceptor。
 */
