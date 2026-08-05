package com.youshangdache.service.impl.coupon;

import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.mapper.coupon.CouponInfoMapper;
import com.youshangdache.mapper.coupon.CustomerCouponMapper;
import com.youshangdache.service.coupon.CouponInfoService;
import com.youshangdache.model.entity.coupon.CouponInfo;
import com.youshangdache.model.entity.coupon.CustomerCoupon;
import com.youshangdache.model.enums.CouponStatusEnum;
import com.youshangdache.model.enums.CouponTypeEnum;
import com.youshangdache.model.enums.CouponUsageThresholdEnum;
import com.youshangdache.model.form.coupon.UseCouponForm;
import com.youshangdache.model.vo.base.PageVo;
import com.youshangdache.model.vo.coupon.AvailableCouponVo;
import com.youshangdache.model.vo.coupon.NoReceiveCouponVo;
import com.youshangdache.model.vo.coupon.NoUseCouponVo;
import com.youshangdache.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {
    @Resource
    private CouponInfoMapper couponInfoMapper;
    @Resource
    private CustomerCouponMapper customerCouponMapper;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        //1 根据id获取乘客优惠券信息
        CustomerCoupon customerCoupon = customerCouponMapper.selectById(useCouponForm.getCustomerCouponId());
        if (customerCoupon == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //2 根据优惠券id获取优惠券信息
        CouponInfo couponInfo = couponInfoMapper.selectById(customerCoupon.getCouponId());
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //3 判断优惠券是否是当前乘客所持有的
        if (!customerCoupon.getCustomerId().equals(useCouponForm.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //4 判断是否具备优惠券使用条件
        BigDecimal reduceAmount = null;
        if (couponInfo.getCouponType() == CouponTypeEnum.CASH) {
            if (couponInfo.getConditionAmount().doubleValue() == 0 &&
                    useCouponForm.getOrderAmount().subtract(couponInfo.getAmount()).doubleValue() > 0) {
                reduceAmount = couponInfo.getAmount();
            }
            if (couponInfo.getConditionAmount().doubleValue() > 0 &&
                    useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                reduceAmount = couponInfo.getAmount();
            }
        }
        if (couponInfo.getCouponType() == CouponTypeEnum.DISCOUNT) {
            BigDecimal discountOrderAmount = useCouponForm.getOrderAmount()
                    .multiply(couponInfo.getDiscount())
                    .divide(new BigDecimal("10"),2, RoundingMode.HALF_UP);
            if (couponInfo.getConditionAmount().doubleValue() == 0) {
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
            if (couponInfo.getConditionAmount().doubleValue() > 0 &&
                    useCouponForm.getOrderAmount().subtract(couponInfo.getConditionAmount()).doubleValue() > 0) {
                reduceAmount = useCouponForm.getOrderAmount().subtract(discountOrderAmount);
            }
        }
        //5 如果满足条件，更新两张表的数据
        if (reduceAmount != null && reduceAmount.doubleValue() > 0) {
            Integer oldUseCount = couponInfo.getUseCount();
            couponInfo.setUseCount(oldUseCount + 1);
            //更新已使用的数量
            couponInfoMapper.updateById(couponInfo);
            //更新customer_coupon
            CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
            updateCustomerCoupon.setId(customerCoupon.getId());
            updateCustomerCoupon.setUsedTime(new Date());
            updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
            customerCouponMapper.updateById(updateCustomerCoupon);
            return reduceAmount;
        }
        return null;
    }

    /**
     * 获取未使用的最佳优惠券信息
     *
     * @param customerId  用户id
     * @param orderAmount 订单金额
     * @return 可用的优惠券列表
     */
    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        //1 创建一个list集合，存储返回的数据
        List<AvailableCouponVo> availableCouponVoList = new ArrayList<>();
        //2 根据乘客id，获取乘客已经领取但是没有使用的优惠券列表
        List<NoUseCouponVo> list = couponInfoMapper.findNoUseList(customerId);
        //3 遍历乘客未使用优惠券列表，得到每个优惠券
        //3 是现金券
        List<NoUseCouponVo> typeList = list.stream()
                .filter(item -> item.getCouponType() == CouponTypeEnum.CASH)
                .toList();
        for (NoUseCouponVo noUseCouponVo : typeList) {
            BigDecimal reduceAmount = noUseCouponVo.getAmount();
            //没有门槛
            if (noUseCouponVo.getConditionAmount().intValue() == CouponUsageThresholdEnum.NO_THRESHOLD.getCode() &&
                    orderAmount.subtract(reduceAmount).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            //有门槛
            if (noUseCouponVo.getConditionAmount().intValue() > CouponUsageThresholdEnum.NO_THRESHOLD.getCode() &&
                    orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }
        //4 折扣券
        List<NoUseCouponVo> typeList2 = list.stream()
                .filter(item -> item.getCouponType() == CouponTypeEnum.DISCOUNT)
                .toList();
        for (NoUseCouponVo noUseCouponVo : typeList2) {
            //折扣之后的金额
            BigDecimal discountAmount = orderAmount.multiply(noUseCouponVo.getDiscount())
                    .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            BigDecimal reduceAmount = orderAmount.subtract(discountAmount);
            //没有门槛
            if (noUseCouponVo.getConditionAmount().intValue() == CouponUsageThresholdEnum.NO_THRESHOLD.getCode()) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
            //有门槛
            if (noUseCouponVo.getConditionAmount().intValue() > CouponUsageThresholdEnum.NO_THRESHOLD.getCode() &&
                    orderAmount.subtract(noUseCouponVo.getConditionAmount()).doubleValue() > 0) {
                availableCouponVoList.add(this.buildBestNoUseCouponVo(noUseCouponVo, reduceAmount));
            }
        }
        //5 把满足条件的优惠券放到list中
        if (!availableCouponVoList.isEmpty()) {
            availableCouponVoList.sort(Comparator.comparing(AvailableCouponVo::getReduceAmount));
        }
        return availableCouponVoList;
    }

    private AvailableCouponVo buildBestNoUseCouponVo(NoUseCouponVo noUseCouponVo, BigDecimal reduceAmount) {
        AvailableCouponVo availableCouponVo = new AvailableCouponVo();
        BeanUtils.copyProperties(noUseCouponVo, availableCouponVo);
        availableCouponVo.setCouponId(noUseCouponVo.getId());
        availableCouponVo.setReduceAmount(reduceAmount);
        return availableCouponVo;
    }

    /**
     * 领取优惠券
     *
     * @param customerId 用户id
     * @param couponId   优惠券id
     * @return true
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean receive(Long customerId, Long couponId) {
        //1、查询优惠券
        CouponInfo couponInfo = this.getById(couponId);
        if (null == couponInfo) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRED_OR_NOT_EXIST);
        }

        //2、优惠券过期日期判断
        if (couponInfo.getExpireTime().before(new Date())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRED);
        }

        //3、校验库存，优惠券领取数量判断
        if (couponInfo.getPublishCount() != 0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        RLock lock = null;
        try {
            // 初始化分布式锁
            //每人领取限制  与 优惠券发行总数 必须保证原子性，使用customerId减少锁的粒度，增加并发能力
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + customerId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (flag) {
                //4、校验每人限领数量
                if (couponInfo.getPerLimit() > 0) {
                    //4.1、统计当前用户对当前优惠券的已经领取的数量
                    long count = customerCouponMapper.selectCount(new LambdaQueryWrapper<CustomerCoupon>().eq(CustomerCoupon::getCouponId, couponId).eq(CustomerCoupon::getCustomerId, customerId));
                    //4.2、校验限领数量
                    if (count >= couponInfo.getPerLimit()) {
                        throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
                    }
                }

                //5、更新优惠券领取数量
                int row;
                if (couponInfo.getPublishCount() == 0) {//发行数量没有限制
                    row = couponInfoMapper.updateReceiveCount(couponId);
                } else {
                    //乐观锁
                    row = couponInfoMapper.updateReceiveCountByLimit(couponId);
                }
                if (row >= 1) {
                    //6、保存领取记录
                    this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //启用的了事务，如果try语句块内的抛出异常，事务就会回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            if (null != lock) {
                lock.unlock();
            }
        }
        throw new GuiguException(ResultCodeEnum.COUPON_LESS);
    }

    private void saveCustomerCoupon(Long customerId, Long couponId, Date expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(CouponStatusEnum.NOT_USED);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(expireTime);
        customerCouponMapper.insert(customerCoupon);
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<UsedCouponVo> pageInfo = couponInfoMapper.findUsedPage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    /**
     * 查询未使用优惠券分页列表
     *
     * @param customerId 用户id
     * @param pageParam  分页参数
     * @return 优惠券分页数据
     */
    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = couponInfoMapper.findNoUsePage(pageParam, customerId);
        return new PageVo(pageInfo.getRecords(), pageInfo.getPages(), pageInfo.getTotal());
    }

    /**
     * 查询未领取优惠券分页列表
     *
     * @param customerId 用户id
     * @param pageParam  分页参数
     * @return 优惠券分页列表
     */
    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> noReceivePage = couponInfoMapper.findNoReceivePage(pageParam, customerId);
        return new PageVo(noReceivePage.getRecords(), noReceivePage.getPages(), noReceivePage.getTotal());
    }
}
