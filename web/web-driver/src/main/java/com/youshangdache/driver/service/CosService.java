package com.youshangdache.driver.service;

import com.youshangdache.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {

    /**
     * 上传证件照到腾讯云私有存储桶， 得有对应的权限才能申请临时访问url
     * @param file 身份证图片
     * @param path
     * @return
     */
    CosUploadVo uploadFile(MultipartFile file,String path);
}
