package com.youshangdache.driver.service;

import com.youshangdache.model.entity.order.OrderMonitor;
import com.youshangdache.model.form.order.OrderMonitorForm;
import org.springframework.web.multipart.MultipartFile;

public interface MonitorService {

    Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm);


}
