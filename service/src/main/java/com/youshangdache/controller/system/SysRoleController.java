package com.youshangdache.controller.system;

import com.youshangdache.model.entity.system.SysRole;
import com.youshangdache.model.query.system.SysRoleQuery;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.system.AssginRoleVo;
import com.youshangdache.service.system.SysRoleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "角色管理")
@RestController
@RequestMapping("/sysRole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Operation(summary = "获取全部角色列表")
    @GetMapping("findAll")
    public List<SysRole> findAll() {
        List<SysRole> roleList = sysRoleService.list();
        return roleList;
    }

    @Operation(summary = "获取分页列表")
    @PostMapping("findPage/{page}/{limit}")
    public PageVo<SysRole> findPage(
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable Long page,

            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable Long limit,

            @Parameter(name = "roleQuery", description = "查询对象", required = false)
            @RequestBody SysRoleQuery roleQuery) {
        Page<SysRole> pageParam = new Page<>(page, limit);
        PageVo<SysRole> pageVo = sysRoleService.findPage(pageParam, roleQuery);
        return pageVo;
    }

    @Operation(summary = "获取")
    @GetMapping("getById/{id}")
    public SysRole getById(@PathVariable Long id) {
        SysRole sysRole = sysRoleService.getById(id);
        return sysRole;
    }

    @Operation(summary = "新增角色")
    @PostMapping("save")
    public Boolean save(@RequestBody @Validated SysRole role) {
        return sysRoleService.save(role);
    }

    @Operation(summary = "修改角色")
    @PutMapping("update")
    public Boolean update(@RequestBody SysRole role) {
        return sysRoleService.updateById(role);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("remove/{id}")
    public Boolean remove(@PathVariable Long id) {
        return sysRoleService.removeById(id);
    }

    @Operation(summary = "根据id列表删除")
    @DeleteMapping("batchRemove")
    public Boolean batchRemove(@RequestBody List<Long> idList) {
        return sysRoleService.removeByIds(idList);
    }

    @Operation(summary = "根据用户获取角色数据")
    @GetMapping("/toAssign/{userId}")
    public Map<String, Object> toAssign(@PathVariable Long userId) {
        Map<String, Object> roleMap = sysRoleService.findRoleByUserId(userId);
        return roleMap;
    }

    @Operation(summary = "根据用户分配角色")
    @PostMapping("/doAssign")
    public Boolean doAssign(@RequestBody AssginRoleVo assginRoleVo) {
        sysRoleService.doAssign(assginRoleVo);
        return true;
    }


}

