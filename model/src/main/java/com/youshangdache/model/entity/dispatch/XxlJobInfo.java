package com.youshangdache.model.entity.dispatch;

import com.youshangdache.model.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * XXL-Job 任务信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "XXL-Job任务信息")
public class XxlJobInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private int id;

    @Schema(description = "执行器主键ID")
    private int jobGroup;

    @Schema(description = "任务描述")
    private String jobDesc;

    @Schema(description = "添加时间")
    private Date addTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "负责人")
    private String author;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "调度类型")
    private String scheduleType;

    @Schema(description = "调度配置，值含义取决于调度类型")
    private String scheduleConf;

    @Schema(description = "调度过期策略")
    private String misfireStrategy;

    @Schema(description = "执行器路由策略")
    private String executorRouteStrategy;

    @Schema(description = "执行器任务Handler名称")
    private String executorHandler;

    @Schema(description = "执行器任务参数")
    private String executorParam;

    @Schema(description = "阻塞处理策略")
    private String executorBlockStrategy;

    @Schema(description = "任务执行超时时间，单位秒")
    private int executorTimeout;

    @Schema(description = "失败重试次数")
    private int executorFailRetryCount;

    @Schema(description = "GLUE类型")
    private String glueType;

    @Schema(description = "GLUE源代码")
    private String glueSource;

    @Schema(description = "GLUE备注")
    private String glueRemark;

    @Schema(description = "GLUE更新时间")
    private Date glueUpdatetime;

    @Schema(description = "子任务ID，多个逗号分隔")
    private String childJobId;

    @Schema(description = "调度状态：0-停止，1-运行")
    private int triggerStatus;

    @Schema(description = "上次调度时间")
    private long triggerLastTime;

    @Schema(description = "下次调度时间")
    private long triggerNextTime;
}
