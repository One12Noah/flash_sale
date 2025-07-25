package cn.wolfcode.common.web.anno;

import java.lang.annotation.*;

/**
 * Created by lanxw
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
}
/**
 * ElementType.METHOD 表示该注解只能应用于方法上。
 * RetentionPolicy.RUNTIME 表示该注解在运行时可用（通过反射可以获取到）
 *
 * 在这个项目中，@RequireLogin 的作用是简单地标记某些方法需要登录校验。
 * 具体的逻辑由拦截器（如 RequireLoginInterceptor）通过反射检查方法是否被该注解标记来实现，
 * 而不需要注解本身携带额外的信息。
 */