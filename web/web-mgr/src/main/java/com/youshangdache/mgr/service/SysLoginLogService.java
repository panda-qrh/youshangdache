package com.youshangdache.mgr.service;

import com.youshangdache.model.entity.system.SysLoginLog;
import com.youshangdache.model.query.system.SysLoginLogQuery;
import com.youshangdache.model.vo.base.PageVo;

public interface SysLoginLogService {

    PageVo<SysLoginLog> findPage(Long page, Long limit, SysLoginLogQuery sysLoginLogQuery);

    /**
     * 记录登录信息
     *
     * @param sysLoginLog
     * @return
     */
    void recordLoginLog(SysLoginLog sysLoginLog);

    SysLoginLog getById(Long id);
}
