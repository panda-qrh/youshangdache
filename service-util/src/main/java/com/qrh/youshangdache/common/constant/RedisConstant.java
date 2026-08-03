package com.qrh.youshangdache.common.constant;

public class RedisConstant {

    /**
     * 用户登录 key 前缀
     */
    public static final String USER_LOGIN_KEY_PREFIX = "user:login:";
    /**
     * 用户登录刷新 key 前缀
     */
    public static final String USER_LOGIN_REFRESH_KEY_PREFIX = "user:login:refresh:";
    /**
     * 用户登录 key 超时时间
     */
    public static final int USER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
    /**
     * 用户登录刷新 key 超时时间
     */
    public static final int USER_LOGIN_REFRESH_KEY_TIMEOUT = 60 * 60 * 24 * 365;

    /**
     * 司机GEO地址key
     */
    public static final String DRIVER_GEO_LOCATION = "driver:geo:location";
    /**
     * 司机接单临时容器前缀
     */
    public static final String DRIVER_ORDER_TEMP_LIST = "driver:order:temp:list:";
    /**
     * 司机订单临时容器过期时间
     */
    public static final long DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME = 1;
    /**
     * 司机订单去重容器key
     */
    public static final String DRIVER_ORDER_REPEAT_LIST = "driver:order:repeat:list:";
    /**
     * 司机订单去重容器过期时间
     */
    public static final long DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME = 16;

//    //订单与任务关联
//    public static final String ORDER_JOB = "order:job:";
//    public static final long ORDER_JOB_EXPIRES_TIME = 15;

    /**
     * 更新订单位置key前缀
     */
    public static final String UPDATE_ORDER_LOCATION = "update:order:location:";
    /**
     * 更新订单位置过期时间
     */
    public static final long UPDATE_ORDER_LOCATION_EXPIRES_TIME = 15;

    /**
     * 订单接单标识key前缀
     */
    public static final String ORDER_ACCEPT_MARK = "order:accept:mark:";
    /**
     * 订单接单标识过期时间
     */
    public static final long ORDER_ACCEPT_MARK_EXPIRES_TIME = 15;

    /**
     * 抢新订单锁key
     */
    public static final String ROB_NEW_ORDER_LOCK = "rob:new:order:lock";
    /**
     * 抢单时 等待获取锁的时间
     */
    public static final long ROB_NEW_ORDER_LOCK_WAIT_TIME = 1;
    /**
     * 抢单时 加锁的最少时间
     */
    public static final long ROB_NEW_ORDER_LOCK_LEASE_TIME = 1;

    /**
     * 优惠券信息key前缀
     */
    public static final String COUPON_INFO = "coupon:info:";

    /**
     * 优惠券分布式锁key前缀
     */
    public static final String COUPON_LOCK = "coupon:lock:";
    /**
     * 优惠券 等待获取锁的时间
     */
    public static final long COUPON_LOCK_WAIT_TIME = 1;
    /**
     * 优惠券 加锁的最少时间
     */
    public static final long COUPON_LOCK_LEASE_TIME = 1;
}
