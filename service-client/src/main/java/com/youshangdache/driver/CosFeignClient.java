package com.youshangdache.driver;

import com.youshangdache.model.vo.driver.CosUploadVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(value = "service-driver")
public interface CosFeignClient {
    /**
     * 上传证件照到腾讯云私有存储桶， 得有对应的权限才能申请临时访问url
     *
     * @param file 身份证图片
     * @param path
     * @return
     */
    @PostMapping(value = "/cos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    CosUploadVo upload(@RequestPart MultipartFile file,
                               @RequestParam(name = "path", defaultValue = "auth") String path);


}