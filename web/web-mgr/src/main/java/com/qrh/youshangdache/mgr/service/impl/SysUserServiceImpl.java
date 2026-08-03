package com.qrh.youshangdache.mgr.service.impl;

import com.qrh.youshangdache.mgr.service.SysUserService;
import com.qrh.youshangdache.model.entity.system.SysUser;
import com.qrh.youshangdache.model.query.system.SysUserQuery;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.system.feign.SysUserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserFeignClient sysUserFeignClient;

    @Override
    public SysUser getById(Long id) {
        return sysUserFeignClient.getById(id).getData();
    }

    @Override
    public void save(SysUser sysUser) {
        sysUserFeignClient.save(sysUser);
    }

    @Override
    public void update(SysUser sysUser) {
        sysUserFeignClient.update(sysUser);
    }

    @Override
    public void remove(Long id) {
        sysUserFeignClient.remove(id);
    }

    @Override
    public PageVo<SysUser> findPage(Long page, Long limit, SysUserQuery sysUserQuery) {
        return sysUserFeignClient.findPage(page, limit, sysUserQuery).getData();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        sysUserFeignClient.updateStatus(id, status);
    }

}
