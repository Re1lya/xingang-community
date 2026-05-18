-- Xingang Community Database Schema
-- Tables: user, shop, shop_type, voucher, seckill_voucher, voucher_order

CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL COMMENT '用户ID，雪花算法',
    `phone`         VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `nick_name`     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `icon`          VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `shop_type` (
    `id`            BIGINT       NOT NULL COMMENT '分类ID',
    `name`          VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `icon`          VARCHAR(512) DEFAULT NULL COMMENT '分类图标URL',
    `sort`          INT          DEFAULT 0 COMMENT '排序',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户分类表';

CREATE TABLE IF NOT EXISTS `shop` (
    `id`            BIGINT       NOT NULL COMMENT '商户ID',
    `name`          VARCHAR(128) NOT NULL COMMENT '商户名称',
    `type_id`       BIGINT       NOT NULL COMMENT '分类ID',
    `area`          VARCHAR(64)  DEFAULT NULL COMMENT '商圈/区域',
    `address`       VARCHAR(256) DEFAULT NULL COMMENT '详细地址',
    `x`             DOUBLE       DEFAULT NULL COMMENT '经度',
    `y`             DOUBLE       DEFAULT NULL COMMENT '纬度',
    `avg_price`     INT          DEFAULT 0 COMMENT '人均价格（分）',
    `score`         DOUBLE       DEFAULT 0 COMMENT '评分',
    `open_hours`    VARCHAR(128) DEFAULT NULL COMMENT '营业时间',
    `comments`      INT          DEFAULT 0 COMMENT '评论数',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

CREATE TABLE IF NOT EXISTS `voucher` (
    `id`            BIGINT       NOT NULL COMMENT '优惠券ID',
    `shop_id`       BIGINT       NOT NULL COMMENT '关联商户ID',
    `title`         VARCHAR(128) NOT NULL COMMENT '优惠券标题',
    `sub_title`     VARCHAR(256) DEFAULT NULL COMMENT '副标题',
    `rules`         VARCHAR(1024) DEFAULT NULL COMMENT '使用规则',
    `pay_value`     BIGINT       NOT NULL COMMENT '支付金额（分）',
    `actual_value`  BIGINT       NOT NULL COMMENT '抵扣金额（分）',
    `type`          TINYINT      DEFAULT 0 COMMENT '类型：0=普通券, 1=秒杀券',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态：0=下架, 1=上架',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE IF NOT EXISTS `seckill_voucher` (
    `voucher_id`    BIGINT       NOT NULL COMMENT '优惠券ID（与voucher表关联）',
    `stock`         INT          NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `begin_time`    DATETIME     NOT NULL COMMENT '秒杀开始时间',
    `end_time`      DATETIME     NOT NULL COMMENT '秒杀结束时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀券表（与voucher通过voucher_id关联）';

CREATE TABLE IF NOT EXISTS `voucher_order` (
    `id`            BIGINT       NOT NULL COMMENT '订单ID，秒杀入口由雪花算法预生成',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `voucher_id`    BIGINT       NOT NULL COMMENT '优惠券ID',
    `status`        TINYINT      DEFAULT 0 COMMENT '订单状态：0=待确认, 1=已创建',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`),
    KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券订单表';

-- ============================================================================
-- Redis Stream 结构说明（非SQL表，仅供运维参考）
-- ============================================================================
--
-- stream.orders (秒杀订单主队列)
--   消费组: order-group
--   消费者: order-consumer-1 (Spring ThreadPoolTaskExecutor)
--   消息体字段:
--     orderId   - 雪花算法预生成的订单ID
--     userId    - 用户ID
--     voucherId - 秒杀券ID
--
-- stream.orders.dlq (死信队列)
--   触发条件: 消息投递次数 >= 3 (deliveryCount >= MAX_DELIVERY_COUNT)
--   写入时机: pending-list 处理前自动扫描路由
--   消息体字段 (包含原始字段 + 失败元信息):
--     orderId          - 原始订单ID
--     userId           - 原始用户ID
--     voucherId        - 原始秒杀券ID
--     originalMessageId - 原始Stream消息ID (用于回溯)
--     failureReason     - 失败原因 (当前固定为 MAX_DELIVERY_EXCEEDED)
--     deliveryCount     - 路由到DLQ前的投递次数
--     movedAt           - 移入DLQ的时间 (ISO格式)
--   DLQ路由流程 (防误丢):
--     1. XPENDING range 扫描 deliveryCount >= 3 的消息
--     2. XADD stream.orders.dlq (写入DLQ)
--     3. XADD 成功 → XACK 原消息; 只有 ack > 0 才算路由成功
--     4. XADD 失败 → 消息留在 pending-list, ERROR 日志
--     5. XADD 成功但 XACK 失败(0/null) → 消息留在 pending-list, ERROR 日志
--        下一轮 pending 扫描可能重复写入 DLQ (接受多一条，不丢一条)
--   幂等(后续增强): 新增 Redis Set seckill:dlq:routed, XADD 前 SISMEMBER 检查 originalMessageId
--
-- 查询命令参考:
--   XPENDING stream.orders order-group                          # pending汇总
--   XPENDING stream.orders order-group - + 50                   # pending详情(含deliveryCount)
--   XRANGE stream.orders messageId messageId                    # 只读查消息体
--   XRANGE stream.orders.dlq - +                                # 查看死信队列全部消息
--   XLEN stream.orders                                          # 主队列长度
--   XLEN stream.orders.dlq                                      # 死信队列长度
