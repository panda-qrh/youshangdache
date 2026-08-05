package com.youshangdache.system;


import com.youshangdache.model.entity.system.SysUser;
import com.youshangdache.model.query.system.SysUserQuery;
import com.youshangdache.model.vo.base.PageVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(value = "service")
public interface SysUserFeignClient {

    /**
     * 获取分页列表
     *
     * @param page
     * @param limit
     * @param sysUserQuery
     * @return
     */
    @PostMapping("/sysUser/findPage/{page}/{limit}")
    PageVo<SysUser> findPage(
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit,
            @RequestBody SysUserQuery sysUserQuery);

    /**
     * 获取用户
     *
     * @param id
     * @return
     */
    @GetMapping("/sysUser/getById/{id}")
    SysUser getById(@PathVariable("id") Long id);

    /**
     * 保存用户
     *
     * @param user
     * @return
     */
    @PostMapping("/sysUser/save")
    Boolean save(@RequestBody SysUser user);

    /**
     * 更新用户
     *
     * @param user
     * @return
     */
    @PutMapping("/sysUser/update")
    Boolean update(@RequestBody SysUser user);

    /**
     * 删除用户
     *
     * @param id
     * @return
     */
    @DeleteMapping("/sysUser/remove/{id}")
    Boolean remove(@PathVariable("id") Long id);

    /**
     * 更新状态
     *
     * @param id
     * @param status
     * @return
     */
    @GetMapping("/sysUser/updateStatus/{id}/{status}")
    Boolean updateStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status);
}

