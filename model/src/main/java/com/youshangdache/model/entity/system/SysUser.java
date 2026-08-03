package com.youshangdache.model.entity.system;

import com.youshangdache.model.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youshangdache.model.enums.AccountStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 系统用户实体
 */
@Data
@Schema(description = "系统用户信息")
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    @Schema(description = "密码")
    @TableField("password")
    private String password;

    @Schema(description = "姓名")
    @TableField("name")
    private String name;

    @Schema(description = "手机号")
    @TableField("phone")
    private String phone;

    @Schema(description = "头像地址")
    @TableField("head_url")
    private String headUrl;

    @Schema(description = "部门ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "岗位ID")
    @TableField("post_id")
    private Long postId;

    @Schema(description = "描述")
    @TableField("description")
    private String description;

	@Schema(description = "状态（1账号正常，2账号被禁用，3账号已被锁定，4账号已被删除）")
    @TableField("status")
    private AccountStatusEnum status;

    @Schema(description = "角色列表（非数据库字段）")
    @TableField(exist = false)
    private List<SysRole> roleList;

    @Schema(description = "岗位名称（非数据库字段）")
    @TableField(exist = false)
    private String postName;

    @Schema(description = "部门名称（非数据库字段）")
    @TableField(exist = false)
    private String deptName;

    @Schema(description = "用户权限标识列表（非数据库字段）")
    @TableField(exist = false)
    private List<String> userPermsList;
}
