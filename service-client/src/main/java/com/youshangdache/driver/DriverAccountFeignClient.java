package com.youshangdache.driver;


import com.youshangdache.model.form.driver.TransferForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver")
public interface DriverAccountFeignClient {

    @PostMapping("/driver/account/transfer")
    public Boolean transfer(@RequestBody TransferForm transferForm);


}