package com.qrh.youshangdache.system.controller;

import com.qrh.youshangdache.model.entity.system.SysDept;
import com.qrh.youshangdache.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping(value="/sysDept")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysDeptController {
	
	@Resource
	private SysDeptService sysDeptService;

	@Operation(summary = "获取")
	@GetMapping("getById/{id}")
	public void getById(@PathVariable Long id) {
		SysDept sysDept = sysDeptService.getById(id);
		return sysDept;
	}

	@Operation(summary = "新增")
	@PostMapping("save")
	public Boolean save(@RequestBody SysDept sysDept) {
		return sysDeptService.save(sysDept);
	}

	@Operation(summary = "修改")
	@PutMapping("update")
	public Boolean update(@RequestBody SysDept sysDept) {
		return sysDeptService.updateById(sysDept);
	}

	@Operation(summary = "删除")
	@DeleteMapping("remove/{id}")
	public Boolean remove(@PathVariable Long id) {
		return sysDeptService.removeById(id);
	}

	@Operation(summary = "获取全部部门节点")
	@GetMapping("findNodes")
	public List<SysDept> findNodes() {
		return sysDeptService.findNodes();
	}

	@Operation(summary = "获取用户部门节点")
	@GetMapping("findUserNodes")
	public List<SysDept> findUserNodes() {
		return sysDeptService.findUserNodes();
	}

	@Operation(summary = "更新状态")
	@GetMapping("updateStatus/{id}/{status}")
	public Boolean updateStatus(@PathVariable Long id, @PathVariable Integer status) {
		sysDeptService.updateStatus(id, status);
		return true;
	}

}

