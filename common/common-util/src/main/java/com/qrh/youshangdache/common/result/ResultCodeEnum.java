package com.qrh.youshangdache.common.result;

import lombok.Getter;

/**
 * 统一返回结果状态信息类
 *
 * <p>编码规则：5 位数字，格式 XXYYY <br>
 * XX  = 业务域编号（前两位） <br>
 * YYY = 域内序号（后三位，每个域预留 1000 个位）
 * </p>
 * <p>域分配：
 * <pre>
 *   10000–10999  系统/通用错误     服务宕机、数据异常、参数校验等
 *   20000–20999  认证/权限         登录、token、密码、验证码
 *   30000–30999  司机模块          司机相关业务错误
 *   40000–40999  订单模块          订单相关业务错误
 *   50000–50999  优惠券模块        优惠券相关业务错误
 *   60000–60999  第三方服务        微信、地图、文件上传、XXL-Job
 *   70000–99999  保留              新增业务域时使用，不预分配
 * </pre>
 * </p>
 * <p>新增错误码规则：<br>
 * 1. 在对应域的区段末尾追加，禁止跨域使用 <br>
 * 2. 若新域尚未分配编号，从保留区段取下一个整十编号 <br>
 * 3. 新增后同步更新本类头部域分配表格 <br>
 * </p>
 */
@Getter
public enum ResultCodeEnum {

    // ==================== 成功 ====================
    SUCCESS(200, "成功"),

    // ==================== 10000-10999 系统/通用错误 ====================
    FAIL(10001, "失败"),
    SERVICE_ERROR(10002, "服务异常"),
    DATA_ERROR(10003, "数据异常"),
    ILLEGAL_REQUEST(10004, "非法请求"),
    REPEAT_SUBMIT(10005, "重复提交"),
    FEIGN_FAIL(10006, "远程调用失败"),
    UPDATE_ERROR(10007, "数据更新失败"),
    ARGUMENT_VALID_ERROR(10008, "参数校验异常"),

    // ==================== 20000-20999 认证/权限 ====================
    SIGN_ERROR(20001, "签名错误"),
    SIGNATURE_OVERDUE(20002, "签名已过期"),
    CAPTCHA_ERROR(20003, "验证码错误"),
    LOGIN_AUTH(20004, "未登陆"),
    PERMISSION(20005, "没有权限"),
    ACCOUNT_ERROR(20006, "账号或密码错误"),
    ACCOUNT_STATUS_ERROR(20007, "账号异常"),
    ACCOUNT_STOPPED(20008, "账号已停用"),
    LOGIN_FAILED(20009, "登陆失败"),
    ACCOUNT_NOT_EXIST(20010, "账号不存在"),
    UNCORRECTED_PHONE_NUMBER(20011, "手机号不正确"),

    // ==================== 30000-30999 司机模块 ====================
    DRIVER_NOT_START_SERVICE(30001, "司机未开启代驾服务，不能更新位置信息"),
    DRIVER_START_LOCATION_DISTANCE_ERROR(30002, "距离代驾起始点1公里以内才能确认"),
    DRIVER_END_LOCATION_DISTANCE_ERROR(30003, "距离代驾终点2公里以内才能确认"),
    DRIVER_NOT_AUTH(30004, "认证通过后才可以开启代驾服务"),
    DRIVER_NOT_FACIAL_RECOGNITION(30005, "当日未进行人脸识别"),
    NODE_ERROR(30006, "该节点下有子节点，不可以删除"),

    // ==================== 40000-40999 订单模块 ====================
    ORDER_SNAP_UP_FAILED(40001, "抢单失败"),
    ORDER_NOT_EXIST(40002, "订单不存在"),

    // ==================== 50000-50999 优惠券模块 ====================
    COUPON_EXPIRED(50001, "优惠券已过期"),
    COUPON_LESS(50002, "优惠券库存不足"),
    COUPON_USER_LIMIT(50003, "超出领取数量"),
    COUPON_EXPIRED_OR_NOT_EXIST(50004, "优惠券不存在或已过期"),

    // ==================== 60000-60999 第三方服务 ====================
    MAP_SERVICE_CALL_FAILED(60001, "地图服务调用失败"),
    PROFITSHARING_FAIL(60002, "分账调用失败"),
    IMAGE_AUDITION_FAIL(60003, "图片审核不通过"),
    FILE_UPLOAD_FAILED(60004, "上传失败"),
    WX_CREATE_ERROR(60005, "微信创建失败"),
    XXL_JOB_ERROR(60006, "任务调度失败");

    private final int code;
    private final String message;

    ResultCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
