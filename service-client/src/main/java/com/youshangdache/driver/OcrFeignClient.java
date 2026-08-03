package com.youshangdache.driver;

import com.youshangdache.model.vo.driver.DriverLicenseOcrVo;
import com.youshangdache.model.vo.driver.IdCardOcrVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "service-driver")
public interface OcrFeignClient {

    @PostMapping(value = "/ocr/idCardOcr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    IdCardOcrVo idCardOcr(@RequestPart("file") MultipartFile file);

    @PostMapping(value = "/ocr/driverLicenseOcr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    DriverLicenseOcrVo driverLicenseOcr(@RequestPart("file") MultipartFile file);
}