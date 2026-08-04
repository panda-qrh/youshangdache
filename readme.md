# 优尚打车系统

优尚打车是一个**代驾服务平台**，用户通过微信小程序预约专业司机来驾驶自己的车辆。项目采用 Spring Cloud 微服务架构，包含 15+ 独立服务，面向微信小程序端用户。

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| **框架** | Spring Boot | 3.0.5 |
| **微服务** | Spring Cloud | 2022.0.2 |
| **云原生** | Spring Cloud Alibaba | 2022.0.0.0-RC2 |
| **服务发现** | Nacos | (via Alibaba) |
| **网关** | Spring Cloud Gateway | |
| **服务调用** | OpenFeign + LoadBalancer | |
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
| **OCR / 人脸核身** | 腾讯云 OCR / CI | 3.1.829 |
| **视频点播** | 腾讯云 VOD | 2.1.4 |
| **分布式事务** | Seata | 1.7.1 |
| **API 文档** | Knife4j (OpenAPI 3) | 4.1.0 |

## 模块结构

```
youshangdache-parent
├── model/                    # 共享领域模型层（实体类、枚举、响应包装类）
├── common-util/              # 共享工具类（异常、常量、拦截器、配置、RSA等）
├── service-client/           # Feign 客户端接口（24个客户端接口）
├── service/                  # 后端微服务
│   ├── service-system/       # 管理系统（用户、角色、菜单、部门、岗位、日志）
│   ├── service-order/        # 订单核心管理（下单、接单、完成、取消）
│   ├── service-driver/       # 司机管理（认证、人脸识别、账号、设置）
│   ├── service-customer/     # 乘客管理（微信登录、个人信息、车辆信息）
│   ├── service-dispatch/     # 订单调度（XXL-Job 定时派单）
│   ├── service-map/          # 地图定位（GPS轨迹、附近司机、MongoDB）
│   ├── service-payment/      # 微信支付（支付、分账）
│   ├── service-coupon/       # 优惠券管理
│   ├── service-rules/        # Drools 规则引擎（计价、奖励、分账规则）
│   └── service-mq/           # RabbitMQ 延迟消息监听
├── web/                      # 前端 API 聚合层
│   ├── web-customer/         # 乘客小程序 API
│   ├── web-driver/           # 司机小程序 API
│   └── web-mgr/              # 管理后台 API
└── server-gateway/           # Spring Cloud Gateway 统一入口
```

## 核心业务流程

### 订单状态机

```
等待接单 → 司机已接单 → 司机已到达 → 更新代驾车辆信息
  → 开始代驾 → 结束代驾 → 未付款 → 已付款 → 订单已结束
```

取消状态：乘客撤单、司机撤单、超时无人接单取消、事故关闭。

### 订单派单

1. XXL-Job 定时轮询未接订单
2. Redis GEO 查询附近可用司机
3. Redisson 分布式锁保证抢单原子性

### 计价规则

通过 Drools 规则引擎动态计算：
- 起步价 19 元，里程费 3-4 元/km
- 等待费 1 元/分钟（10 分钟起）
- 超里程加价 1 元/km（12km 起）

## 环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.0
- Redis
- RabbitMQ（含延迟消息插件）
- MongoDB
- Nacos 配置中心
- XXL-Job 调度中心

## 构建与运行

```bash
# 构建全部模块
mvn clean package -DskipTests

# 启动顺序：Nacos → MySQL → Redis → RabbitMQ → MongoDB → 后端服务 → 网关
```

- 网关为统一入口，所有前端请求均通过 `server-gateway`
- 默认激活 `dev` profile
- 各服务配置通过 Nacos 配置中心管理

## 项目状态

- 部分控制器为预留接口，功能待完善
- 面向微信小程序，客户端通过 Gateway 访问
