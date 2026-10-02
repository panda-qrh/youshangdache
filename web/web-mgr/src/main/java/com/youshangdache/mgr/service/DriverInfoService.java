package com.youshangdache.mgr.service;

public interface DriverInfoService {

    /**
     * 后台审核司机认证信息
     *
     * @param driverId   司机id
     * @param authStatus 审核结果：1 审核中，2 认证通过，-1 认证未通过
     * @return true
     */
    Boolean updateDriverAuthStatus(Long driverId, Integer authStatus);
}
