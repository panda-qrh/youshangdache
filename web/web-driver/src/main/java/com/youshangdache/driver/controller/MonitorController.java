package com.youshangdache.driver.controller;

import com.youshangdache.common.result.Result;
import com.youshangdache.driver.service.MonitorService;
import com.youshangdache.model.form.order.OrderMonitorForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "监控接口管理")
@RestController
@RequestMapping(value = "/monitor")
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorController {
    @Resource
    private MonitorService monitorService;

    @Operation(summary = "上传记录")
    @PostMapping("/upload")
    public Result<Boolean> saveMonitorRecord(@RequestPart MultipartFile file, OrderMonitorForm orderMonitorForm) {
        return Result.ok(monitorService.upload(file, orderMonitorForm));
    }

}

