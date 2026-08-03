package com.youshangdache.common.handler;

import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.Result;
import com.youshangdache.common.result.ResultCodeEnum;
import feign.codec.DecodeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理类，拦截所有 Controller 层抛出的异常并统一返回格式
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 拦截所有未明确匹配的异常
     *
     * @param e 异常对象
     * @return 统一格式的错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> error(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.fail();
    }

    /**
     * 拦截业务自定义异常（GuiguException）
     *
     * @param e 自定义异常对象，携带 code 和 message
     * @return 携带业务错误码的响应
     */
    @ExceptionHandler(GuiguException.class)
    public Result<?> error(GuiguException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.build(null, e.getCode(), e.getMessage());
    }

    /**
     * 拦截 Feign 远程调用解码异常
     *
     * @param e 解码异常对象
     * @return 远程调用失败的响应
     */
    @ExceptionHandler(DecodeException.class)
    public Result<?> error(DecodeException e) {
        log.error("Feign远程调用失败: {}", e.getMessage(), e);
        return Result.fail(ResultCodeEnum.FEIGN_FAIL);
    }

    /**
     * 拦截参数校验异常（@Validated 作用于普通参数时抛出 BindException）
     *
     * @param e 参数绑定异常对象
     * @return 携带字段级错误信息的响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("参数异常: {}", e.getMessage(), e);
        return Result.build(null, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }

    /**
     * 拦截参数校验异常（@Validated 作用于实体类时抛出 MethodArgumentNotValidException）
     *
     * @param e 方法参数校验异常对象
     * @return 携带字段级错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> error(MethodArgumentNotValidException e) {
        return handleBindException(e.getBindingResult());
    }

    /**
     * 拦截表单绑定校验异常（@ModelAttribute 参数校验失败时抛出 BindException）
     *
     * @param e 表单绑定异常对象
     * @return 携带字段级错误信息的响应
     */
    @ExceptionHandler(BindException.class)

    public Result<?> error(BindException e) {
        return handleBindException(e.getBindingResult());
    }

    /**
     * 从 BindingResult 中提取字段错误信息，构建错误响应
     *
     * @param result 参数绑定结果
     * @return 携带字段级错误信息的响应
     */
    private Result<?> handleBindException(BindingResult result) {
        Map<String, Object> errorMap = new HashMap<>();
        List<FieldError> fieldErrors = result.getFieldErrors();
        for (FieldError error : fieldErrors) {
            log.error("字段校验失败: field={}, msg={}", error.getField(), error.getDefaultMessage());
            errorMap.put(error.getField(), error.getDefaultMessage());
        }
        return Result.build(errorMap, ResultCodeEnum.ARGUMENT_VALID_ERROR);
    }
}
