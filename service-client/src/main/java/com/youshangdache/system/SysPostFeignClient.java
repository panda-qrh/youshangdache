package com.youshangdache.system;

import com.youshangdache.model.entity.system.SysPost;
import com.youshangdache.model.query.system.SysPostQuery;
import com.youshangdache.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service")
public interface SysPostFeignClient {

    @PostMapping("/sysPost/findPage/{page}/{limit}")
    PageVo<SysPost> findPage(
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit,
            @RequestBody SysPostQuery sysPostQuery);

    @GetMapping("/sysPost/getById/{id}")
    SysPost getById(@PathVariable Long id);

    @GetMapping("/sysPost/findAll")
    List<SysPost> findAll();

    @PostMapping("/sysPost/save")
    Boolean save(@RequestBody SysPost sysPost);

    @PutMapping("/sysPost/update")
    Boolean update(@RequestBody SysPost sysPost);

    @DeleteMapping("/sysPost/remove/{id}")
    Boolean remove(@PathVariable("id") Long id);

    @GetMapping("/sysPost/updateStatus/{id}/{status}")
    Boolean updateStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status);

}

