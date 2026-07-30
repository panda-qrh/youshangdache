package com.qrh.youshangdache.common.result;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 全局统一返回结果类
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全局统一返回结果")
public class Result<T> {

    /** 返回码 */
    @Schema(description = "返回码", example = "200")
    private int code;

    /** 返回消息 */
    @Schema(description = "返回消息", example = "成功")
    private String message;

    /** 返回数据 */
    @Schema(description = "返回数据")
    private T data;

    /**
     * 构建完整的响应对象（内部唯一构造入口）
     *
     * @param body           需要返回的数据
     * @param resultCodeEnum 统一返回结果状态信息枚举类对象
     * @param <T>            泛型，任意类型
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        return new Result<>(resultCodeEnum.getCode(), resultCodeEnum.getMessage(), body);
    }

    /**
     * 构建自定义code和message的响应对象
     *
     * @param body    需要返回的数据
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     泛型，任意类型
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> build(T body, int code, String message) {
        return new Result<>(code, message, body);
    }

    /**
     * 操作成功（无数据）
     *
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> ok() {
        return build(null, ResultCodeEnum.SUCCESS);
    }

    /**
     * 操作成功
     *
     * @param data 需要返回的数据
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    /**
     * 操作成功（带自定义消息，无数据）
     *
     * @param message 自定义消息
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> ok(String message) {
        return build(null, ResultCodeEnum.SUCCESS.getCode(), message);
    }

    /**
     * 操作失败（无数据）
     *
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> fail() {
        return build(null, ResultCodeEnum.FAIL);
    }

    /**
     * 操作失败（带数据）
     *
     * @param data 需要返回的数据
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }

    /**
     * 操作失败（带自定义消息）
     *
     * @param message 自定义错误消息
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> fail(String message) {
        return build(null, ResultCodeEnum.FAIL.getCode(), message);
    }

    /**
     * 根据枚举直接构建失败响应
     *
     * @param resultCodeEnum 统一返回结果状态信息枚举类对象
     * @return JSON格式的响应数据
     */
    public static <T> Result<T> fail(ResultCodeEnum resultCodeEnum) {
        return build(null, resultCodeEnum);
    }
}
