package com.youshangdache.driver.controller;

import com.youshangdache.driver.service.CosService;
import com.youshangdache.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value="/cos")
public class CosController {
    @Resource
    private CosService cosService;

    /**
     * 上传证件照到腾讯云私有存储桶， 得有对应的权限才能申请临时访问url
     * @param file
     * @param path
     * @return
     */
    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public CosUploadVo upload(@RequestPart MultipartFile file,
                               @RequestParam(name="path",defaultValue = "auth")String path){
        return cosService.upload(file,path);
    }

}

