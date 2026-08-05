package com.youshangdache.controller.system;

import com.youshangdache.common.annotation.Log;
import com.youshangdache.model.enums.BusinessTypeEnum;
import com.youshangdache.common.util.MD5;
import com.youshangdache.model.entity.system.SysUser;
import com.youshangdache.model.query.system.SysUserQuery;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.service.system.SysUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "用户管理")
@RestController
@RequestMapping("/sysUser")
@CrossOrigin
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "获取分页列表")
    @PostMapping("findPage/{page}/{limit}")
    public PageVo<SysUser> findPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit,

            @Parameter(name = "userQuery", description = "查询对象", required = false)
            @RequestBody SysUserQuery sysUserQuery) {
        Page<SysUser> pageParam = new Page<>(page, limit);
        PageVo<SysUser> pageVo = sysUserService.findPage(pageParam, sysUserQuery);
        return pageVo;
    }

    @Operation(summary = "获取用户")
    @GetMapping("getById/{id}")
    public SysUser getById(@PathVariable Long id) {
        SysUser sysUser = sysUserService.getById(id);
        return sysUser;
    }

    @Log(title = "用户管理", businessType = BusinessTypeEnum.INSERT)
    @Operation(summary = "保存用户")
    @PostMapping("save")
    public Boolean save(@RequestBody SysUser user) {
        user.setPassword(MD5.encrypt(user.getPassword()));
        return sysUserService.save(user);
    }

    @Operation(summary = "更新用户")
    @PutMapping("update")
    public Boolean updateById(@RequestBody SysUser sysUser) {
        return sysUserService.updateById(sysUser);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("remove/{id}")
    public Boolean remove(@PathVariable Long id) {
        return sysUserService.removeById(id);
    }

    @Operation(summary = "更新状态")
    @GetMapping("updateStatus/{id}/{status}")
    public Boolean updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        sysUserService.updateStatus(id, status);
        return true;
    }
}

