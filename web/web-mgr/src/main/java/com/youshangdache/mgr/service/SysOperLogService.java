package com.youshangdache.mgr.service;

import com.youshangdache.model.entity.system.SysOperLog;
import com.youshangdache.model.query.system.SysOperLogQuery;
import com.youshangdache.model.vo.base.PageVo;

public interface SysOperLogService {

    PageVo<SysOperLog> findPage(Long page, Long limit, SysOperLogQuery sysOperLogQuery);

    /**
     * 保存系统日志记录
     */
    void saveSysLog(SysOperLog sysOperLog);

    SysOperLog getById(Long id);
}
