package com.youshangdache.map.service;

import com.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.youshangdache.model.vo.map.DrivingLineVo;

public interface MapService {
    /**
     * 计算驾驶线路
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);
}
