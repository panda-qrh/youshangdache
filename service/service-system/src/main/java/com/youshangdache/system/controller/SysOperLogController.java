package com.youshangdache.system.controller;

import com.youshangdache.model.entity.system.SysOperLog;
import com.youshangdache.model.query.system.SysOperLogQuery;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.system.service.SysOperLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 *
 * @author qy
 *
 */
@Tag(name = "系统操作日志管理")
@RestController
@RequestMapping(value="/sysOperLog")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysOperLogController {
	
	@Resource
	private SysOperLogService sysOperLogService;

	@Operation(summary = "获取分页列表")
	@PostMapping("findPage/{page}/{limit}")
	public PageVo<SysOperLog> findPage(
		@Parameter(name = "page", description = "当前页码", required = true)
		@PathVariable Long page,
	
		@Parameter(name = "limit", description = "每页记录数", required = true)
		@PathVariable Long limit,
	
		@Parameter(name = "sysOperLogVo", description = "查询对象", required = false)
		@RequestBody SysOperLogQuery sysOperLogQuery) {
		Page<SysOperLog> pageParam = new Page<>(page, limit);
		PageVo<SysOperLog> pageVo = sysOperLogService.findPage(pageParam, sysOperLogQuery);
		return pageVo;
	}

	@Operation(summary = "获取")
	@GetMapping("getById/{id}")
	public SysOperLog getById(@PathVariable Long id) {
		SysOperLog sysOperLog = sysOperLogService.getById(id);
		return sysOperLog;
	}

	@Operation(summary = "记录日志")
	@PostMapping("saveSysLog")
	public Boolean saveSysLog(@RequestBody SysOperLog sysOperLog) {
		sysOperLogService.saveSysLog(sysOperLog);
		return true;
	}

}

