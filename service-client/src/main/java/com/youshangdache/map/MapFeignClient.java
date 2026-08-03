package com.youshangdache.map;

import com.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.youshangdache.model.vo.map.DrivingLineVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-map")
public interface MapFeignClient {
    /**
     * 计算最佳驾驶路线-司乘同显
     *
     * @param calculateDrivingLineForm 起点坐标和终点坐标对象
     * @return 路线
     */
    @PostMapping("/map/calculateDrivingLine")
    DrivingLineVo calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm);
}