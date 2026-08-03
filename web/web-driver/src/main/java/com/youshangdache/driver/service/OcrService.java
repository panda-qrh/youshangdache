package com.youshangdache.driver.service;

import com.youshangdache.model.vo.driver.DriverLicenseOcrVo;
import com.youshangdache.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    IdCardOcrVo idCardOcr(MultipartFile file);

    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);

}
