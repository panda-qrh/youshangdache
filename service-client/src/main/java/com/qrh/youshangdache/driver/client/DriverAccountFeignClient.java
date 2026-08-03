package com.qrh.youshangdache.driver.client;


import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.model.form.driver.TransferForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface DriverAccountFeignClient {

    @PostMapping("/driver/account/transfer")
    public Result<Boolean> transfer(@RequestBody TransferForm transferForm);


}