package com.youshangdache.driver.service.impl;

import com.youshangdache.driver.mapper.DriverAccountDetailMapper;
import com.youshangdache.driver.mapper.DriverAccountMapper;
import com.youshangdache.driver.service.DriverAccountService;
import com.youshangdache.model.entity.driver.DriverAccount;
import com.youshangdache.model.entity.driver.DriverAccountDetail;
import com.youshangdache.model.enums.TradeTypeEnum;
import com.youshangdache.model.form.driver.TransferForm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverAccountServiceImpl extends ServiceImpl<DriverAccountMapper, DriverAccount> implements DriverAccountService {
    @Resource
    private DriverAccountMapper driverAccountMapper;
    @Resource
    private DriverAccountDetailMapper driverAccountDetailMapper;


    /**
     * 司机账户入账
     *
     * <p>按 tradeNo 幂等：同一个交易号只会入账一次。
     * 账户加钱与写流水必须在同一个本地事务里，否则会出现"加了钱但没流水"或"有流水但没加钱"。</p>
     *
     * @param transferForm 转账信息
     * @return true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transfer(TransferForm transferForm) {
        Long count = driverAccountDetailMapper.selectCount(new LambdaQueryWrapper<DriverAccountDetail>().eq(DriverAccountDetail::getTradeNo, transferForm.getTradeNo()));
        if (count > 0) {
            return true;
        }
        int row = driverAccountMapper.add(transferForm.getDriverId(), transferForm.getAmount());
        if (row < 1) {
            //账户不存在时不会更新任何行，这里直接暴露问题，避免悄悄丢掉一笔入账
            throw new IllegalStateException("司机账户不存在，入账失败，driverId=" + transferForm.getDriverId());
        }

        DriverAccountDetail driverAccountDetail = new DriverAccountDetail();
        BeanUtils.copyProperties(transferForm, driverAccountDetail);
        //TransferForm.tradeType 是 Integer，而 DriverAccountDetail.tradeType 是 TradeTypeEnum，
        //BeanUtils 对类型不兼容的属性会静默跳过，导致流水表的 trade_type 永远是 null，这里手工转换。
        if (transferForm.getTradeType() != null) {
            driverAccountDetail.setTradeType(TradeTypeEnum.of(transferForm.getTradeType()));
        }
        driverAccountDetailMapper.insert(driverAccountDetail);
        return true;
    }
}
