package com.qrh.youshangdache.system.controller;

import com.qrh.youshangdache.model.entity.system.SysMenu;
import com.qrh.youshangdache.model.vo.system.AssginMenuVo;
import com.qrh.youshangdache.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/sysMenu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @Operation(summary = "获取菜单")
    @GetMapping("findNodes")
    public List<SysMenu> findNodes() {
        List<SysMenu> list = sysMenuService.findNodes();
        return list;
    }

    @Operation(summary = "新增菜单")
    @PostMapping("save")
    public Boolean save(@RequestBody SysMenu permission) {
        return sysMenuService.save(permission);
    }

    @Operation(summary = "修改菜单")
    @PutMapping("update")
    public Boolean update(@RequestBody SysMenu permission) {
        return sysMenuService.updateById(permission);
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("remove/{id}")
    public Boolean remove(@PathVariable Long id) {
        return sysMenuService.removeById(id);
    }

    @Operation(summary = "根据角色获取菜单")
    @GetMapping("toAssign/{roleId}")
    public List<SysMenu> toAssign(@PathVariable Long roleId) {
        List<SysMenu> list = sysMenuService.findSysMenuByRoleId(roleId);
        return list;
    }

    @Operation(summary = "给角色分配权限")
    @PostMapping("/doAssign")
    public Boolean doAssign(@RequestBody AssginMenuVo assginMenuVo) {
        sysMenuService.doAssign(assginMenuVo);
        return true;
    }
}

