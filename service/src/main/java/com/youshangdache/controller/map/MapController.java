package com.youshangdache.controller.map;

import com.youshangdache.service.map.MapService;
import com.youshangdache.model.form.map.CalculateDrivingLineForm;
import com.youshangdache.model.vo.map.DrivingLineVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "地图API接口管理")
@RestController
@RequestMapping("/map")
public class MapController {
    @Resource
    private MapService mapService;

    /**
     * 计算驾驶线路
     * @param calculateDrivingLineForm
     * @return
     */
    @Operation(summary = "计算驾驶线路")
    @PostMapping("/calculateDrivingLine")
    public DrivingLineVo calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return mapService.calculateDrivingLine(calculateDrivingLineForm);
    }

}

