package com.qrh.youshangdache.mgr.service.impl;

import com.qrh.youshangdache.mgr.service.SysOperLogService;
import com.qrh.youshangdache.model.entity.system.SysOperLog;
import com.qrh.youshangdache.model.query.system.SysOperLogQuery;
import com.qrh.youshangdache.model.vo.base.PageVo;
import com.qrh.youshangdache.system.feign.SysOperLogFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysOperLogServiceImpl implements SysOperLogService {

	@Autowired
	private SysOperLogFeignClient sysOperLogFeignClient;

	@Override
	public PageVo<SysOperLog> findPage(Long page, Long limit, SysOperLogQuery sysOperLogQuery) {
		return sysOperLogFeignClient.findPage(page, limit, sysOperLogQuery).getData();
	}

	@Override
	public void saveSysLog(SysOperLog sysOperLog) {
		sysOperLogFeignClient.saveSysLog(sysOperLog);
	}

	@Override
	public SysOperLog getById(Long id) {
		return sysOperLogFeignClient.getById(id).getData();
	}
}
