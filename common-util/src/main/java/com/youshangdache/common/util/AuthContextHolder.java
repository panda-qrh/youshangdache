package com.youshangdache.common.util;

/**
 * 获取当前线程的用户信息帮助类
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();

    /**
     * 设置用户ID
     *
     * @param _userId 用户ID
     */
    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return userId.get();
    }

    /**
     * 删除用户ID
     */
    public static void removeUserId() {
        userId.remove();
    }

}
