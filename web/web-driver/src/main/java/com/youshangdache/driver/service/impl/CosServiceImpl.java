package com.youshangdache.driver.service.impl;

import com.youshangdache.driver.CosFeignClient;
import com.youshangdache.driver.service.CosService;
import com.youshangdache.model.vo.driver.CosUploadVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CosServiceImpl implements CosService {

    @Resource
    private CosFeignClient cosFeignClient;

    /**
     * 上传证件照到腾讯云私有存储桶， 得有对应的权限才能申请临时访问url
     *
     * @param file 身份证图片
     * @param path
     * @return
     */
    @Override
    public CosUploadVo uploadFile(MultipartFile file, String path) {
        return cosFeignClient.upload(file, path);
    }
}
