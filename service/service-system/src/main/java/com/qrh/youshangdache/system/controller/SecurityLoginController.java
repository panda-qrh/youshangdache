package com.qrh.youshangdache.system.controller;

import com.alibaba.fastjson.JSON;
import com.qrh.youshangdache.model.entity.system.SysUser;
import com.qrh.youshangdache.model.vo.system.LoginVo;
import com.qrh.youshangdache.system.service.SysMenuService;
import com.qrh.youshangdache.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "security登录管理")
@RestController
@RequestMapping(value="/securityLogin")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SecurityLoginController {
	
	@Resource
	private SysUserService sysUserService;

	@Resource
	private SysMenuService sysMenuService;

	@Operation(summary = "模拟登录")
	@PostMapping("login")
	public void login(@RequestBody LoginVo loginVo) {
		log.info(JSON.toJSONString(loginVo));
	}

	@Operation(summary = "根据用户名获取用户信息")
	@GetMapping("getByUsername/{username}")
	public SysUser getByUsername(@PathVariable String username) {
		return sysUserService.getByUsername(username);
	}

	@Operation(summary = "获取用户按钮权限")
	@GetMapping("findUserPermsList/{userId}")
	public List<String> findUserPermsList(@PathVariable Long userId) {
		return sysMenuService.findUserPermsList(userId);
	}

	@Operation(summary = "获取用户信息")
	@GetMapping("getUserInfo/{userId}")
	public Map<String, Object> getUserInfo(@PathVariable Long userId) {
		Map<String, Object> map = sysUserService.getUserInfo(userId);
		return map;
	}
}

