package com.qrh.youshangdache.driver.service.impl;

import com.qrh.youshangdache.driver.feign.OcrFeignClient;
import com.qrh.youshangdache.driver.service.OcrService;
import com.qrh.youshangdache.model.vo.driver.DriverLicenseOcrVo;
import com.qrh.youshangdache.model.vo.driver.IdCardOcrVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    @Resource
    private OcrFeignClient ocrFeignClient;

    @Override
    public IdCardOcrVo idCardOcr(MultipartFile file) {
        return ocrFeignClient.idCardOcr(file).getData();
    }

    @Override
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) {
        return ocrFeignClient.driverLicenseOcr(file).getData();
    }
}
