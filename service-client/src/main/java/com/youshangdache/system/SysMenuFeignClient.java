package com.youshangdache.system;

import com.youshangdache.model.entity.system.SysMenu;
import com.youshangdache.model.vo.system.AssginMenuVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service-system")
public interface SysMenuFeignClient {


    /**
     * 获取菜单
     *
     * @return
     */
    @GetMapping("/sysMenu/findNodes")
    List<SysMenu> findNodes();

    @PostMapping("/sysMenu/save")
    Boolean save(@RequestBody SysMenu sysMenu);

    @PutMapping("/sysMenu/update")
    Boolean update(@RequestBody SysMenu permission);

    @DeleteMapping("/sysMenu/remove/{id}")
    Boolean remove(@PathVariable Long id);

    /**
     * 根据角色获取菜单
     *
     * @param roleId
     * @return
     */
    @GetMapping("/sysMenu/toAssign/{roleId}")
    List<SysMenu> toAssign(@PathVariable Long roleId);

    /**
     * 给角色分配权限
     *
     * @param assginMenuVo
     * @return
     */
    @PostMapping("/sysMenu/doAssign")
    Boolean doAssign(@RequestBody AssginMenuVo assginMenuVo);
}

