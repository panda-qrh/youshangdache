package com.youshangdache.mgr.service.impl;

import com.youshangdache.mgr.service.SysDeptService;
import com.youshangdache.model.entity.system.SysDept;
import com.youshangdache.system.SysDeptFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysDeptServiceImpl implements SysDeptService {

    @Autowired
    private SysDeptFeignClient sysDeptFeignClient;


    @Override
    public List<SysDept> findNodes() {
        return sysDeptFeignClient.findNodes();
    }

    @Override
    public List<SysDept> findUserNodes() {
        return sysDeptFeignClient.findUserNodes();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        sysDeptFeignClient.updateStatus(id, status);
    }

    @Override
    public SysDept getById(Long id) {
        return sysDeptFeignClient.getById(id);
    }

    @Override
    public void save(SysDept sysDept) {
        sysDeptFeignClient.save(sysDept);
    }

    @Override
    public void update(SysDept sysDept) {
        sysDeptFeignClient.update(sysDept);
    }

    @Override
    public void remove(Long id) {
        sysDeptFeignClient.remove(id);
    }
}

