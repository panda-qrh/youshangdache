package com.youshangdache.driver.mapper;

import com.youshangdache.model.entity.driver.DriverAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface DriverAccountMapper extends BaseMapper<DriverAccount> {


    /**
     * 给司机账户加钱
     *
     * <p>两个参数都必须加 {@link Param}，否则依赖编译期的 -parameters 参数名保留，
     * 一旦编译配置变化（或部署到不保留参数名的构建环境）就会报参数绑定失败。</p>
     *
     * @param driverId 司机id
     * @param amount   金额
     * @return 受影响行数
     */
    int add(@Param("driverId") Long driverId, @Param("amount") BigDecimal amount);

}
