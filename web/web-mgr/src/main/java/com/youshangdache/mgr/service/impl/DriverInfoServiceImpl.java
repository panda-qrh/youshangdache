package com.youshangdache.mgr.service.impl;

import com.youshangdache.driver.DriverInfoFeignClient;
import com.youshangdache.mgr.service.DriverInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl implements DriverInfoService {

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverAuthStatus(Long driverId, Integer authStatus) {
        return driverInfoFeignClient.updateDriverAuthStatus(driverId, authStatus);
    }
}
