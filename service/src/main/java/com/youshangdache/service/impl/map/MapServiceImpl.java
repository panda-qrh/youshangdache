package com.youshangdache.service.impl.map;

import com.alibaba.fastjson.JSONObject;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.service.map.MapService;
import com.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.youshangdache.model.vo.map.DrivingLineVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RefreshScope
public class MapServiceImpl implements MapService {
    @Resource
    private RestTemplate restTemplate;

    @Value("tencent.cloud.map")
    private String key;

    /**
     * 计算驾驶路线
     *
     * @param calculateDrivingLineForm
     * @return
     */
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        //请求腾讯提供的接口，最返回需要的结果
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
        //封装传递的参数
        Map<String, String> map = Map.of(
                "from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude(),
                "to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude(),
                "key", key
        );
        //使用restTemplate调用
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        //返回处理结果

        if (result == null || result.getIntValue("status") != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_SERVICE_CALL_FAILED);
        }
        //返回获取路线信息
        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        drivingLineVo.setDistance(
                route.getBigDecimal("distance").divide(new BigDecimal("1000"), 2, RoundingMode.UP)
        );
        //路线
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        return drivingLineVo;
    }
}
