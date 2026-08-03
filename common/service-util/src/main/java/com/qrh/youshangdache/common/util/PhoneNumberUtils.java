package com.qrh.youshangdache.common.util;

/**
 * @author QRH
 * @date 2023-03-08 16:02
 * @description 手机号码格式校验工具类，用户校验手机号码是否符合中国大陆手机号码格式
 */
public class PhoneNumberUtils {
    /**
     * 验证手机号码是否正确
     *
     * @param phoneNumber 手机号码字符串
     * @return 如果手机号码格式正确返回true，否则返回false
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        // 定义手机号码的正则表达式
        String regex = "^1[3-9]\\d{9}$";
        // 使用正则表达式进行匹配
        return phoneNumber.matches(regex);
    }
}
