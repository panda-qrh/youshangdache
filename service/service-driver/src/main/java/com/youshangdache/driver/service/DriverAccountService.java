package com.youshangdache.driver.service;

import com.youshangdache.model.entity.driver.DriverAccount;
import com.youshangdache.model.form.driver.TransferForm;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DriverAccountService extends IService<DriverAccount> {


    Boolean transfer(TransferForm transferForm);
}
