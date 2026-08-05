package com.youshangdache.system;

import com.youshangdache.model.entity.system.SysDept;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service")
public interface SysDeptFeignClient {

    @GetMapping("/sysDept/getById/{id}")
    SysDept getById(@PathVariable Long id);

    @PostMapping("/sysDept/save")
    Boolean save(@RequestBody SysDept sysDept);

    @PutMapping("/sysDept/update")
    Boolean update(@RequestBody SysDept sysDept);

    @DeleteMapping("/sysDept/remove/{id}")
    Boolean remove(@PathVariable Long id);

    /**
     * 获取全部部门节点
     *
     * @return
     */
    @GetMapping("/sysDept/findNodes")
    List<SysDept> findNodes();

    /**
     * 获取用户部门节点
     *
     * @return
     */
    @GetMapping("/sysDept/findUserNodes")
    List<SysDept> findUserNodes();

    /**
     * 更新状态
     *
     * @param id
     * @param status
     * @return
     */
    @GetMapping("/sysDept/updateStatus/{id}/{status}")
    Boolean updateStatus(@PathVariable Long id, @PathVariable Integer status);

}

