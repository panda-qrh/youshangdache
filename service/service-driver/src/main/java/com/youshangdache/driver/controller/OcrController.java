package com.youshangdache.driver.controller;

import com.youshangdache.driver.service.OcrService;
import com.youshangdache.model.vo.driver.DriverLicenseOcrVo;
import com.youshangdache.model.vo.driver.IdCardOcrVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云识别接口管理")
@RestController
@RequestMapping(value="/ocr")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrController {
	@Resource
    private OcrService ocrService;

    @Operation(summary = "身份证识别")
    @PostMapping("/idCardOcr")
    public IdCardOcrVo idCardOcr(@RequestPart("file") MultipartFile file) {
        return ocrService.idCardOcr(file);
    }

    @Operation(summary = "驾驶证识别")
    @PostMapping("/driverLicenseOcr")
    public DriverLicenseOcrVo driverLicenseOcr(@RequestPart("file") MultipartFile file) {
        return ocrService.driverLicenseOcr(file);
    }

}

