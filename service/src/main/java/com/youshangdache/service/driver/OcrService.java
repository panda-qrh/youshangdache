package com.youshangdache.service.driver;

import com.youshangdache.model.vo.driver.DriverLicenseOcrVo;
import com.youshangdache.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * 身份识别
     * @param file
     * @return
     */
    IdCardOcrVo idCardOcr(MultipartFile file);

    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);
}
