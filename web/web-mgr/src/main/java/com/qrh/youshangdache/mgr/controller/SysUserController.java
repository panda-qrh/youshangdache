package com.qrh.youshangdache.mgr.controller;

import com.qrh.youshangdache.common.annotation.Log;
import com.qrh.youshangdache.model.enums.BusinessTypeEnum;
import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.common.util.MD5;
import com.qrh.youshangdache.mgr.service.SysUserService;
import com.qrh.youshangdache.model.entity.system.SysUser;
import com.qrh.youshangdache.model.query.system.SysUserQuery;
import com.qrh.youshangdache.model.vo.base.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "用户管理")
@RestController
@RequestMapping("/sysUser")
@CrossOrigin
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "获取分页列表")
    @PreAuthorize("hasAuthority('bnt.sysUser.list')")
    @PostMapping("{page}/{limit}")
    public Result<PageVo<SysUser>> findPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit,

            @Parameter(name = "userQuery", description = "查询对象", required = false)
            @RequestBody SysUserQuery sysUserQuery) {
        return Result.ok(sysUserService.findPage(page, limit, sysUserQuery));
    }

    @Operation(summary = "获取用户")
    @PreAuthorize("hasAuthority('bnt.sysUser.list')")
    @GetMapping("getById/{id}")
    public Result getById(@PathVariable Long id) {
        SysUser sysUser = sysUserService.getById(id);
        return Result.ok(sysUser);
    }

    @Log(title = "用户管理", businessType = BusinessTypeEnum.INSERT)
    @Operation(summary = "保存用户")
    @PreAuthorize("hasAuthority('bnt.sysUser.add')")
    @PostMapping("save")
    public Result save(@RequestBody SysUser sysUser) {
        sysUser.setPassword(MD5.encrypt(sysUser.getPassword()));
        sysUserService.save(sysUser);
        return Result.ok();
    }

    @Log(title = "用户管理", businessType = BusinessTypeEnum.UPDATE)
    @Operation(summary = "更新用户")
    @PreAuthorize("hasAuthority('bnt.sysUser.update')")
    @PutMapping("update")
    public Result update(@RequestBody SysUser sysUser) {
        sysUserService.update(sysUser);
        return Result.ok();
    }

    @Log(title = "用户管理", businessType = BusinessTypeEnum.DELETE)
    @Operation(summary = "删除用户")
    @PreAuthorize("hasAuthority('bnt.sysUser.remove')")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        sysUserService.remove(id);
        return Result.ok();
    }

    @Log(title = "用户管理", businessType = BusinessTypeEnum.STATUS)
    @Operation(summary = "更新状态")
    @GetMapping("updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.ok();
    }
}

