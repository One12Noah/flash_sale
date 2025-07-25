package cn.wolfcode.common.exception;

import cn.wolfcode.common.web.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by wolfcode-lanxw
 */
public class CommonControllerAdvice {
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public Result handleBusinessException(BusinessException ex){
        return Result.error(ex.getCodeMsg());
    }
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleDefaultException(Exception ex){
        ex.printStackTrace();//在控制台打印错误消息.
        return Result.defaultError();
    }
}

/**
 * 本项目的统一异常处理是通过继承 `CommonControllerAdvice` 类并结合 Spring 的全局异常处理机制实现的。以下是实现方式的关键点：
 *
 * 1. **基础异常处理类**：
 *    - `CommonControllerAdvice` 是一个基础的异常处理类，定义了两个异常处理方法：
 *      - `handleBusinessException`：处理 `BusinessException` 类型的业务异常，返回统一的错误响应。
 *      - `handleDefaultException`：处理所有其他异常，打印堆栈信息并返回默认错误响应。
 *
 * 2. **全局异常处理类**：
 *    - 各模块通过创建自己的全局异常处理类（如 `UAAControllerAdvice`、`PayControllerAdvice`、`IntergralControllerAdvice`），并继承 `CommonControllerAdvice`，实现模块级别的统一异常处理。
 *    - 使用 `@ControllerAdvice` 注解标记这些类，使其成为全局异常处理器。
 *
 * 3. **统一响应格式**：
 *    - 异常处理方法返回 `Result` 对象，确保所有异常的响应格式一致，便于前端处理。
 *    - `Result` 类提供了静态方法 `error` 和 `defaultError`，分别用于构造自定义错误响应和默认错误响应。
 *
 * 4. **业务异常封装**：
 *    - `BusinessException` 类封装了业务异常信息，包含一个 `CodeMsg` 对象，用于描述错误码和错误信息。
 *    - 各模块可以通过自定义 `CodeMsg`（如 `SeckillCodeMsg`）定义模块特定的错误信息。
 *    自定义的业务异常类 BusinessException，继承自 Java 的 RuntimeException，用于在项目中封装和抛出业务相关的异常信息
 *
 * 通过这种设计，项目实现了模块化的统一异常处理，既保证了代码的复用性，又提高了可维护性和一致性。
 */
