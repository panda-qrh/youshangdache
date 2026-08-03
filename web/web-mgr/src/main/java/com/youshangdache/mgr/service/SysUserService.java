package com.youshangdache.mgr.service;


import com.youshangdache.model.entity.system.SysUser;
import com.youshangdache.model.query.system.SysUserQuery;
import com.youshangdache.model.vo.base.PageVo;

public interface SysUserService {

    SysUser getById(Long id);

    void save(SysUser sysUser);

    void update(SysUser sysUser);

    void remove(Long id);

    PageVo<SysUser> findPage(Long page, Long limit, SysUserQuery sysUserQuery);

    void updateStatus(Long id, Integer status);


}
