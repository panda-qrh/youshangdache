package com.youshangdache.controller.system;

import com.youshangdache.model.entity.system.SysLoginLog;
import com.youshangdache.model.query.system.SysLoginLogQuery;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.service.system.SysLoginLogService;
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
@Tag(name = "系统登录日志管理")
@RestController
@RequestMapping(value="/sysLoginLog")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysLoginLogController {
	
	@Resource
	private SysLoginLogService sysLoginLogService;

	@Operation(summary = "获取分页列表")
	@PostMapping("findPage/{page}/{limit}")
	public PageVo<SysLoginLog> findPage(
		@Parameter(name = "page", description = "当前页码", required = true)
		@PathVariable Long page,
	
		@Parameter(name = "limit", description = "每页记录数", required = true)
		@PathVariable Long limit,
	
		@Parameter(name = "sysLoginLogVo", description = "查询对象", required = false)
		@RequestBody SysLoginLogQuery sysLoginLogQuery) {
		Page<SysLoginLog> pageParam = new Page<>(page, limit);
		PageVo<SysLoginLog> pageModel = sysLoginLogService.findPage(pageParam, sysLoginLogQuery);
		return pageModel;
	}

	@Operation(summary = "获取")
	@GetMapping("getById/{id}")
	public SysLoginLog getById(@PathVariable Long id) {
		SysLoginLog sysLoginLog = sysLoginLogService.getById(id);
		return sysLoginLog;
	}

	@Operation(summary = "记录登录日志")
	@PostMapping("recordLoginLog")
	public Boolean recordLoginLog(@RequestBody SysLoginLog sysLoginLog) {
		sysLoginLogService.recordLoginLog(sysLoginLog);
		return true;
	}

}

