# CLAUDE.md

## 项目概述

**优尚打车（Youshangdache）** 是一个代驾服务平台，用户通过微信小程序预约专业司机来驾驶自己的车辆。采用 Spring Cloud 微服务架构，包含 15+ 独立服务，面向微信小程序端用户。

- **基包**: `com.youshangdache`
- **Java**: 17
- **Spring Boot**: 3.0.5
- **作者**: QRH
- **项目状态**: 开发中，部分模块功能不完整

---

## 模块结构

```
youshangdache-parent (父POM)
├── model/                       # 共享领域模型层（实体类、枚举、响应包装类）
├── common-util/                 # 共享工具类（异常、常量、拦截器、配置、RSA等）
├── service-client/              # Feign 客户端接口（共享库，24个客户端接口）
├── service/                     # 后端微服务（父POM）
│   ├── service-system/          # 管理系统（用户、角色、菜单、部门、岗位、操作日志）
│   ├── service-order/           # 订单核心管理（下单、改派、接单、完成、取消）
│   ├── service-driver/          # 司机管理（认证、人脸识别、账号、设置）
│   ├── service-customer/        # 乘客管理（微信登录、个人信息、车辆信息）
│   ├── service-dispatch/        # 订单调度（XXL-Job 定时派单）
│   ├── service-map/             # 地图定位服务（GPS轨迹、附近司机、MongoDB）
│   ├── service-payment/         # 微信支付集成（支付、分账）
│   ├── service-coupon/          # 优惠券管理
│   ├── service-rules/           # Drools 规则引擎（计价、奖励、分账规则）
│   └── service-mq/              # RabbitMQ 延迟消息监听
├── web/                         # 前端 API 聚合层（父POM）
│   ├── web-customer/            # 乘客小程序 API（无本地数据源，通过 Feign 调用后端服务）
│   ├── web-driver/              # 司机小程序 API（同上）
│   └── web-mgr/                 # 管理后台 API（同上）
└── server-gateway/              # Spring Cloud Gateway 统一入口
```

---

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| **框架** | Spring Boot | 3.0.5 |
| **微服务** | Spring Cloud | 2022.0.2 |
| **云原生** | Spring Cloud Alibaba | 2022.0.0.0-RC2 |
| **服务发现** | Nacos | (via Alibaba) |
| **网关** | Spring Cloud Gateway | (via Cloud) |
| **服务调用** | OpenFeign + Spring Cloud LoadBalancer | |
| **熔断降级** | Sentinel | (via Alibaba) |
| **ORM** | MyBatis-Plus | 3.5.3.1 |
| **数据库** | MySQL | 8.0.30 |
| **缓存** | Redis + Redisson | 3.23.3 |
| **消息队列** | RabbitMQ（含延迟消息插件） | |
| **规则引擎** | Drools | 8.41.0.Final |
| **定时任务** | XXL-Job | 2.4.0 |
| **支付** | 微信支付 API v3 | 0.2.11 |
| **小程序** | weixin-java-miniapp | 4.5.5.B |
| **地图定位** | 腾讯云 SDK + MongoDB | 3.1.322 |
| **文件存储** | MinIO + 腾讯云 COS | 8.5.2 / 5.6.155 |
| **OCR** | 腾讯云 OCR | 3.1.829 |
| **人脸核身** | 腾讯云 CI | |
| **视频点播** | 腾讯云 VOD | 2.1.4 |
| **分布式事务** | Seata | 1.7.1（已配置但未启用） |
| **API 文档** | Knife4j (OpenAPI 3) | 4.1.0 |
| **JSON** | FastJSON 2 | 2.0.41 |
| **工具** | Lombok, Joda-Time, Commons-IO | |

---

## 架构设计

### 分层架构

每个服务内部采用标准四层结构：

```
Controller (REST 接口)
    → Service (接口 + 实现，接口继承 IService<T>，实现继承 ServiceImpl<M,T>)
        → Mapper (MyBatis-Plus 接口，复杂查询使用 XML)
            → Database (MySQL)
```

### 三层 Web 架构

web 模块作为前端 API 聚合层，不包含业务逻辑，仅通过 Feign 调用后端服务，按用户端提供不同 API 面：

| 模块 | 目标用户 | 数据源 |
|---|---|---|
| web-customer | 乘客小程序 | 无本地数据源 |
| web-driver | 司机小程序 | 无本地数据源 |
| web-mgr | 管理后台 | 无本地数据源 |

所有 web 模块使用 `@EnableDiscoveryClient + @EnableFeignClients`，并排除 `DataSourceAutoConfiguration`。

### 服务间通信

- Feign 客户端统一定义在 `service-client` 模块中（24 个客户端接口）
- 通过 `@FeignClient` + Spring Cloud LoadBalancer 实现负载均衡
- 所有 web 模块和跨服务调用均通过 Feign 进行

### API 网关

- Spring Cloud Gateway 作为统一入口
- 自定义 Token 认证：`TokenLoginFilter`（登录，Redis 存储 Token，100天 TTL）+ `TokenAuthenticationFilter`（请求鉴权）
- `AuthInterceptor` 从请求头提取 Token，将用户 ID 存入 `AuthContextHolder` ThreadLocal
- `AuthGlobalFilter` 为占位符，后续用于全局鉴权逻辑

### Web 模块鉴权

- `@Login` 自定义注解标记需要登录的接口
- `LoginAspect`（AOP Around 通知）提取 Token → Redis 验证 → 设置用户 ID 到 `AuthContextHolder` ThreadLocal

---

## 领域模型与核心流程

### 核心实体

| 领域 | 实体 |
|---|---|
| **订单** | OrderInfo, OrderBill, OrderStatusLog, OrderTrack, OrderComment, OrderMonitor, OrderMonitorRecord, OrderProfitsharing |
| **司机** | DriverInfo, DriverAccount, DriverAccountDetail, DriverFaceRecognition, DriverLoginLog, DriverSet |
| **乘客** | CustomerInfo, CustomerCar, CustomerLoginLog |
| **优惠券** | CouponInfo, CustomerCoupon |
| **支付** | PaymentInfo, ProfitsharingInfo |
| **规则** | CancelRule, FeeRule, ProfitsharingRule, RewardRule |
| **调度** | OrderJob, XxlJobLog |
| **地图** | OrderServiceLocation |
| **系统** | SysUser, SysRole, SysMenu, SysPost, SysDept 等 |

### 基础实体 (BaseEntity)

所有实体继承 `BaseEntity`，提供通用字段：
- `id`（自增主键）
- `createTime`（插入自动填充）
- `updateTime`
- `isDeleted`（逻辑删除，`@TableLogic`）
- `param`（Map，用于动态查询参数）

### 订单状态机 (OrderStatusEnum)

```
1  WAITING_ACCEPT               → 等待接单
2  ACCEPTED                     → 司机已接单
3  DRIVER_ARRIVED               → 司机已到达
12 UPDATE_CAR_INFO              → 更新代驾车辆信息
4  START_SERVICE                → 开始代驾
5  END_SERVICE                  → 结束代驾
6  ORDER_UNPAID                 → 未付款
7  ORDER_PAID                   → 已付款
8  ORDER_FINISHED               → 订单已结束
9  ORDER_CANCELED_BY_USER       → 乘客撤单
10 ORDER_CANCELED_BY_DRIVER     → 司机撤单
11 ORDER_CLOSED_CASE_ACCIDENT   → 事故关闭
-1 ORDER_CANCELED_WITH_NO_DRIVER_ACCEPT_ORDER → 超时无人接单取消
```

### 订单派单流程

1. XXL-Job 定时轮询未接订单
2. Redis GEO 查询附近可用司机
3. Redisson 分布式锁保证抢单原子性
4. Redis 中司机临时订单队列（15分钟 TTL）

### 订单取消机制（双保险）

- RabbitMQ 延迟交换机（15分钟） + Redisson 延迟队列
- `DelayReceiver`（service-mq）监听取消消息
- `RedisDelayHandler`（service-order）使用 Redisson 阻塞队列

### 计价规则（Drools）

三个 `.drl` 规则文件位于 `service-rules/src/main/resources/rules/`：
- **FeeRule.drl**: 起步价 19 元，里程费 3-4 元/km，等待费 1 元/分钟（10分钟起），超里程加价 1 元/km（12km起）
- **RewardRule.drl**: 夜间5单后奖励 5 元，白天10单后奖励 2 元
- **ProfitsharingRule.drl**: 平台抽成 16-20%，司机收入，10% 司机税费，0.6% 微信支付手续费

### 支付流程

1. 微信支付 JSAPI 下单
2. 支付成功后触发 RabbitMQ 消息
3. 延迟消息触发分账逻辑（Drools 计算分账金额）
4. 调用微信分账接口完成分账

### 地图定位

- MongoDB 存储 GPS 轨迹点（OrderServiceLocation）
- Redis GEO 存储实时司机位置，支持附近司机搜索
- 服务结束后从 MongoDB 计算实际里程

---

## 编码规范

### 基础约定

| 约定 | 说明 |
|---|---|
| **Lombok** | 所有实体使用 `@Data`，Controller/Service 使用 `@Slf4j` |
| **MyBatis-Plus** | `@TableName` 指定表名，`@TableField` 指定字段映射，`@TableId(type = AUTO)` 自增主键，`@TableLogic` 逻辑删除 |
| **Swagger** | Controller 使用 `@Tag` + `@Operation`，实体使用 `@Schema` |
| **API 响应** | 统一包装为 `Result<T>`，使用 `Result.ok()` / `Result.fail()` |
| **错误码** | `ResultCodeEnum`，5位数字格式 `XXYYY`：10000-10999 系统级，20000-20999 认证，30000-30999 司机，40000-40999 订单，50000-50999 优惠券，60000-60999 第三方 |
| **异常** | 业务异常使用 `GuiguException(ResultCodeEnum)`，全局异常处理器 `GlobalExceptionHandler` |
| **鉴权** | 需要登录的方法添加 `@Login` 注解 |
| **审计** | 操作日志使用 `@Log(title, businessType, operatorType)` |
| **注入方式** | 使用 `@Resource`（Jakarta），不使用 `@Autowired` |
| **枚举** | `@Getter @AllArgsConstructor` + `@EnumValue` + `@JsonValue` / `@JsonCreator` |
| **常量** | 集中在 `common-util` 的 `constant` 包下 |
| **ThreadLocal** | `AuthContextHolder` 存储当前请求用户 ID |

### 代码组织

- 每个模块的 Controller 使用 `@Tag` 分组
- Mapper XML 文件放在 `resources/mapper/` 下
- 配置文件按环境区分：`application.yml`（通用）+ `application-dev.yml`（开发）
- Nacos 配置中心存放敏感配置（数据库、Redis、Seata 等）

---

## 构建与运行

### 前置要求

- JDK 17
- Maven 3.6+
- MySQL 8.0
- Redis
- RabbitMQ（含延迟消息插件）
- MongoDB
- Nacos 配置中心（`192.168.200.130:8848`）
- XXL-Job 调度中心
- Seata TC（`127.0.0.1:8091`，已配置但未启用）

### 构建

```bash
# 从根目录构建全部模块
mvn clean package -DskipTests

# 构建单个模块
cd service/service-order && mvn clean package -DskipTests
```

### 运行

1. 确保 Nacos、MySQL、Redis、RabbitMQ、MongoDB 已启动
2. 将各模块配置上传到 Nacos（或使用本地配置文件）
3. 启动顺序：Nacos → MySQL → Redis → RabbitMQ → MongoDB → 后端服务 → 网关
4. 网关是统一入口，所有前端请求均通过 `server-gateway`

### 开发环境

- 激活 profile: `dev`
- 端口分配见各模块 `application.yml`（默认 8xxx 系列）

---

## 注意事项

1. **项目处于开发中**：部分控制器为空实现，`DataSourceConfig` 注释掉了，web 模块部分服务接口未实现
2. **Web 模块是纯转发层**：所有业务逻辑在后端服务中，web 模块仅做 API 聚合和鉴权
3. **Seata 已配置但未启用**：`DataSourceConfig` 被注释，分布式事务功能暂不可用
4. **取消机制为双保险设计**：RabbitMQ 延迟交换机 + Redisson 延迟队列并行运行
5. **项目源自"听书"模板**：Seata tx group 名称为 `tingshu-tx-group`，部分配置模式继承自之前的项目
