package com.youshangdache.driver.controller;

import com.youshangdache.driver.service.DriverAccountService;
import com.youshangdache.model.form.driver.TransferForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "司机账户API接口管理")
@RestController
@RequestMapping(value = "/driver/account")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverAccountController {

    @Resource
    private DriverAccountService driverAccountService;

    @Operation(summary = "转账")
    @PostMapping("/transfer")
    public Boolean transfer(@RequestBody TransferForm transferForm) {
        return driverAccountService.transfer(transferForm);
    }
}

