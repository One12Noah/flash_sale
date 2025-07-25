package cn.wolfcode.filters;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.redis.CommonRedisKey;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Created by lanxw
 * 定义全局过滤器，功能如下:
 * 1.把客户端真实IP通过请求同的方式传递给微服务
 * 2.在请求头中添加FEIGN_REQUEST的请求头，值为0，标记请求不是Feign调用，而是客户端调用
 * 3.刷新Token的有效时间
 */
@Component
public class CommonFilter implements GlobalFilter {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * pre拦截逻辑（请求之前）
         * 在请求去到微服务之前，做了两个处理
         * 1.把客户端真实IP通过请求同的方式传递给微服务
         * 2.在请求头中添加FEIGN_REQUEST的请求头，值为0，标记请求不是Feign调用，而是客户端调用
         */
        ServerHttpRequest request = exchange.getRequest().mutate().
                header(CommonConstants.REAL_IP,exchange.getRequest().getRemoteAddress().getHostString()).
                header(CommonConstants.FEIGN_REQUEST_KEY,CommonConstants.FEIGN_REQUEST_FALSE).
                build();
        return chain.filter(exchange.mutate().request(request).build()).then(Mono.fromRunnable(()->{
            /**
             * post拦截逻辑（请求处理完成之后）
             * 在请求执行完微服务之后,需要刷新token在redis的时间
             * 判断token不为空 && Redis还存在这个token对于的key,这时候需要延长Redis中对应key的有效时间.
             */
            String token,redisKey;
            if(!StringUtils.isEmpty(token =exchange.getRequest().getHeaders().getFirst(CommonConstants.TOKEN_NAME))
                    && redisTemplate.hasKey(redisKey = CommonRedisKey.USER_TOKEN.getRealKey(token))){
                redisTemplate.expire(redisKey, CommonRedisKey.USER_TOKEN.getExpireTime(), CommonRedisKey.USER_TOKEN.getUnit());
            }
        }));
    }
}
/**
 * 假设 Redis 中的键 userToken:abc123 当前剩余过期时间为 10 分钟，
 * 执行 redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES) 后，
 * 该键的过期时间会被重置为 30 分钟，而不是在原有基础上增加 30 分钟。
 *
 *
 * 添加前缀（如 userToken:）有以下优点：
 * 添加前缀可以区分不同业务场景的键，避免冲突。
 * 直观地表明键的用途。
 * 方便地对相关键进行批量操作（如删除、查询等）。
 * 有助于代码的可维护性和团队协作。
 *
 * pre 写在 chain.doFilter() 之前，post 写在之后
 */