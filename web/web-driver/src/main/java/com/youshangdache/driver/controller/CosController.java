package com.youshangdache.driver.controller;

import com.youshangdache.common.annotation.Login;
import com.youshangdache.common.result.Result;
import com.youshangdache.driver.service.CosService;
import com.youshangdache.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
     * @param file 身份证图片
     * @param path
     * @return 图片存储路径及回显地址
     */
    @Operation(summary = "文件上传")
    @Login
    @PostMapping("/upload")
    public Result<CosUploadVo> upload(@RequestPart MultipartFile file,
                                      @RequestParam(name="path",defaultValue = "auth")String path){
        CosUploadVo cosUploadVo=cosService.uploadFile(file,path);
        return Result.ok(cosUploadVo);
    }

}

