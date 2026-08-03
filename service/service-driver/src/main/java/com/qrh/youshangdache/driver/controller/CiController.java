package com.qrh.youshangdache.driver.controller;

import com.qrh.youshangdache.driver.service.CiService;
import com.qrh.youshangdache.model.vo.order.TextAuditingVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "腾讯云CI审核接口管理")
@RestController
@RequestMapping(value="/cos")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CiController {
	@Resource
    private CiService ciService;

	@Operation(summary = "文本审核")
    @PostMapping("/textAuditing")
    public TextAuditingVo textAuditing(@RequestBody String content){
	    return ciService.textAuditing(content);
    }

}

