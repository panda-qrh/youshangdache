package com.youshangdache.service.impl.system;

import com.youshangdache.model.entity.system.SysPost;
import com.youshangdache.model.query.system.SysPostQuery;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.mapper.system.SysPostMapper;
import com.youshangdache.service.system.SysPostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

	@Resource
	private SysPostMapper sysPostMapper;

	@Override
	public PageVo<SysPost> findPage(Page<SysPost> pageParam, SysPostQuery sysPostQuery) {
		IPage<SysPost> pageInfo = sysPostMapper.selectPage(pageParam, sysPostQuery);
		return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
	}

	@Override
	public void updateStatus(Long id, Integer status) {
		SysPost sysPost = this.getById(id);
		sysPost.setStatus(status);
		this.updateById(sysPost);
	}

	@Override
	public List<SysPost> findAll() {
		List<SysPost> sysPostList = this.list(new LambdaQueryWrapper<SysPost>().eq(SysPost::getStatus, 1));
		return sysPostList;
	}

}
