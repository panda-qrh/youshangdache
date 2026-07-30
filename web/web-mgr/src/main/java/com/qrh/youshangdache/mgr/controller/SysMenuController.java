package com.qrh.youshangdache.mgr.controller;

import com.qrh.youshangdache.common.annotation.Log;
import com.qrh.youshangdache.model.enums.BusinessTypeEnum;
import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.mgr.service.SysMenuService;
import com.qrh.youshangdache.model.entity.system.SysMenu;
import com.qrh.youshangdache.model.vo.system.AssginMenuVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/sysMenu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @PreAuthorize("hasAuthority('bnt.sysMenu.list')")
    @Operation(summary = "获取菜单")
    @GetMapping("findNodes")
    public Result findNodes() {
        List<SysMenu> list = sysMenuService.findNodes();
        return Result.ok(list);
    }

    @Log(title = "菜单管理", businessType = BusinessTypeEnum.INSERT)
    @PreAuthorize("hasAuthority('bnt.sysMenu.add')")
    @Operation(summary = "新增菜单")
    @PostMapping("save")
    public Result save(@RequestBody SysMenu permission) {
        sysMenuService.save(permission);
        return Result.ok();
    }

    @Log(title = "菜单管理", businessType = BusinessTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('bnt.sysMenu.update')")
    @Operation(summary = "修改菜单")
    @PutMapping("update")
    public Result update(@RequestBody SysMenu permission) {
        sysMenuService.update(permission);
        return Result.ok();
    }

    @Log(title = "菜单管理", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('bnt.sysMenu.remove')")
    @Operation(summary = "删除菜单")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        sysMenuService.remove(id);
        return Result.ok();
    }

    @PreAuthorize("hasAuthority('bnt.sysRole.assignAuth')")
    @Operation(summary = "根据角色获取菜单")
    @GetMapping("toAssign/{roleId}")
    public Result toAssign(@PathVariable Long roleId) {
        List<SysMenu> list = sysMenuService.toAssign(roleId);
        return Result.ok(list);
    }

    @Log(title = "角色管理", businessType = BusinessTypeEnum.ASSIGN)
    @PreAuthorize("hasAuthority('bnt.sysRole.assignAuth')")
    @Operation(summary = "给角色分配权限")
    @PostMapping("/doAssign")
    public Result doAssign(@RequestBody AssginMenuVo assginMenuVo) {
        sysMenuService.doAssign(assginMenuVo);
        return Result.ok();
    }
}

